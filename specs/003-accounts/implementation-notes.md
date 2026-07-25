# Implementation Notes — Accounts (003)

This file supplements `data-model.md` and `contracts/account.openapi.yaml` with the *why* and
*how* behind each piece, for manual backend implementation. It assumes you already have the
Category feature (`002-categories`) implemented and working — every section below points at the
existing pattern to extend rather than re-explaining the framework from scratch.

Stack versions for the doc links: **Spring Boot 4.0.6** (BOM-managed — Spring Framework 7.0.x,
Spring Data MongoDB 5.x, Spring Security 6.x/7.x are pulled in via the BOM, no separate pins in
`libs.versions.toml`).

---

## 1. Where this feature lives (hexagonal layering)

Same skeleton as Category, substitute `Account` for `Category`:

```
domain/model/Account.java                          — plain record, no persistence annotations
domain/command/account/{Create,Update}AccountCommand.java
domain/commandhandler/account/{Create,Update,Deactivate,Reconcile}AccountCommandHandler.java
domain/queryhandler/account/{ListAccounts,GetAccount,GetAccountBalanceHistory}QueryHandler.java
domain/port/{AccountRepository,AccountReadRepository}.java
application/controller/AccountController.java
application/response/AccountResponse.java (+ BalanceHistoryResponse, BalancePoint)
application/mapper/AccountResponseMapper.java
infrastructure/persistence/document/AccountDocument.java
infrastructure/persistence/AccountMongoRepository.java
infrastructure/adapter/AccountRepositoryAdapter.java
infrastructure/mapper/AccountMapper.java (MapStruct, domain ↔ document)
```

Reference: `api/CLAUDE.md` hexagonal diagram — domain has zero Spring/Mongo imports; only
`application` and `infrastructure` know about either.

## 2. Domain model vs. persistence document

`Category.java` is a plain Lombok `@Builder` record with no annotations at all; the Mongo shape
lives in a **separate** `CategoryDocument extends BaseDocument` class
(`infrastructure/persistence/document/CategoryDocument.java`) with `@Document(collection =
"categories")`, `@Getter/@Setter/@SuperBuilder/@FieldNameConstants`. `BaseDocument` supplies `@Id`,
`@CreatedDate`, `@LastModifiedDate`.

For Accounts, mirror this exactly:
- `Account` (domain record): `id, ownerId, name, type, institution, initialBalance, color,
  isActive, createdAt`. Derived `currentBalance` is **not** a field here — see §5.
- `AccountDocument extends BaseDocument`: same fields minus `id`/`createdAt` (inherited), plus
  `@Version private Long version;` (see §6 — Category doesn't use `@Version` today, but the
  contract requires optimistic locking here because reconcile is a concurrent-edit path).

`AccountMapper` (MapStruct interface) does document ↔ domain, same shape as `CategoryMapper`.
`AccountResponseMapper` (application layer) does domain → `AccountResponse`, dropping `ownerId`
per the "least disclosure" rule in `api/CLAUDE.md`.

## 3. Tenancy — where `ownerId` comes from

There is **no custom `ReactiveSecurityContextHolder` plumbing** in this codebase — don't add any.
The JWT subject is injected straight into controller methods:

```java
@GetMapping
public Flux<CategoryResponse> listCategories(
    @AuthenticationPrincipal(expression = "subject") String ownerId) { ... }
```

`ownerId` is then threaded explicitly through every command/query handler call
(`handler.handle(command, ownerId)`), never re-derived from a Reactor `Context` inside the domain
or infrastructure layer. Reproduce this exactly for every `AccountController` endpoint.

The JWT itself arrives via an `accessToken` cookie, not an `Authorization` header — see the
custom `bearerTokenConverter` in `infrastructure/config/SecurityConfig.java`. You don't need to
touch this for Accounts; it's already wired for the whole app.

Docs: [Spring Security reactive OAuth2 resource server — JWT](https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/jwt.html)
(concept: `ReactiveJwtDecoder`, how the `Jwt` principal is built and exposed to
`@AuthenticationPrincipal`).

## 4. Repository & queries

`CategoryMongoRepository` is a *thin* marker interface — `ReactiveMongoRepository<CategoryDocument,
String>`, no custom query methods. All actual owner-scoped filtering happens in
`CategoryRepositoryAdapter` using `ReactiveMongoTemplate` directly with `Query`/`Criteria`:

```java
reactiveMongoTemplate.find(
    Query.query(Criteria.where(CategoryDocument.Fields.ownerId).is(ownerId)),
    CategoryDocument.class)
  .map(categoryMapper::toDomain);
```

Do the same for `AccountRepositoryAdapter implements AccountRepository, AccountReadRepository`:
`findAllByOwner(ownerId, includeInactive)`, `findByIdAndOwner(id, ownerId)`, `save(account)`,
`deactivate(id, ownerId)` (an `updateFirst` setting `isActive=false`, not a document delete —
this is a soft delete per the contract, same idea as Category's `deleteWithReassignment` but
simpler: no children to reassign).

Duplicate account name → same `DuplicateKeyException` → `ConflictException` mapping used in
`CategoryRepositoryAdapter.save()`:
```java
.onErrorMap(DuplicateKeyException.class, ex -> new ConflictException("Account name already exists"))
```

Docs: [Spring Data MongoDB — Querying Documents](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-query-operations.html),
[MongoDB Repositories](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/repositories.html).

## 5. `currentBalance` and `balance-history` — the first aggregation pipeline in this codebase

**Nothing in the codebase calls `.aggregate(...)` yet** (grepped `ReactiveMongoTemplate` usage
repo-wide — only `find`/`findOne`/`updateMulti`/index-ops appear). This is genuinely new ground,
so budget extra time here; there's no in-repo example to copy, only the general pattern below.

Per `api/CLAUDE.md`: *"Derived values... are computed by the read side via Mongo aggregation and
never persisted."* This means:
- `currentBalance` and `balance-history` live in a **query handler**
  (`domain/queryhandler/account/`), not a command handler, and not stored on `AccountDocument`.
- The read port (`AccountReadRepository` or a dedicated `AccountBalanceProjection`) exposes
  something like `Mono<Long> currentBalance(String accountId, String ownerId)` and
  `Flux<BalancePoint> balanceHistory(String accountId, String ownerId, int days)`, implemented
  in the adapter with `ReactiveMongoTemplate.aggregate(...)`.

**Important**: this aggregates over the `transactions` collection, which doesn't exist yet
(Feature 2.1 / `specs/004-transactions`). You're forward-referencing that schema — from
`specs/004-transactions/data-model.md`, the relevant fields are `ownerId`, `accountId`, `type`
(`DEBIT`/`CREDIT`), `amount` (Int64 centavos, always positive), `date`. The index
`{ ownerId: 1, accountId: 1, date: -1 }` already anticipates this query shape.

Conceptual pipeline for `currentBalance`:
```java
Aggregation.newAggregation(
    Aggregation.match(Criteria.where("ownerId").is(ownerId).and("accountId").is(accountId)),
    Aggregation.group("type").sum("amount").as("total")
)
```
then combine the `CREDIT` and `DEBIT` group totals in Java:
`initialBalance + creditTotal - debitTotal` (never do this arithmetic inside the pipeline with
`$divide`/floats — stay in `Long` centavos per the root `CLAUDE.md` money rule; simple `$sum` is
integer-safe).

For `balance-history` (last 30 daily closing balances), the concept is a running/cumulative sum
per day: `$match` the account + a 30-day date window, `$group by { $dateToString: { format:
"%Y-%m-%d", date: "$date" } }` summing signed amounts per day, then compute the cumulative
running balance in the reactive chain (`Flux.scan(...)`, seeded with `initialBalance` as of 30
days ago) rather than inside Mongo — a `$setWindowFields` cumulative-sum stage is possible but
adds real complexity for a 30-point series; doing the running sum in Java after grouping by day is
simpler and just as correct at this data volume. **Since Transactions isn't built yet, this
endpoint has nothing to aggregate over until 2.1 ships — you can either stub it (return
`initialBalance` for all 30 days) or defer implementing it until Transactions lands. Either way,
keep the `AccountReadRepository` port signature so the controller endpoint exists per the
contract.**

Docs: [Spring Data MongoDB — Aggregation Framework Support](https://docs.spring.io/spring-data/mongodb/reference/mongodb/aggregation-framework.html)
(concept: `Aggregation`/`AggregationResults`, `newAggregation(...)`, executed via
`ReactiveMongoTemplate.aggregate(...)`), [MongoDB Manual — Aggregation Pipeline](https://www.mongodb.com/docs/manual/core/aggregation-pipeline/)
(the underlying `$match`/`$group`/`$sum` stage semantics, framework-agnostic).

## 6. Optimistic locking (`@Version`) — new for this feature

Category doesn't use `@Version` (nothing concurrently edits a category). Accounts need it because
`reconcile` and `update` can race. Add `@Version private Long version;` to `AccountDocument`
(Spring Data MongoDB auto-manages the field: increments on save, rejects a stale write with
`OptimisticLockingFailureException`). Map that exception to `ConflictException` (409) in
`AccountRepositoryAdapter`, same `onErrorMap` pattern as the duplicate-key case in §4. The
`data-model.md` field is called `_version` at the BSON level — use `@Field("_version")` alongside
`@Version` if you want the wire name to differ from the Java field name (matches the existing
`@FieldNameConstants`/document convention).

Docs: [Spring Data MongoDB — optimistic locking](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping-conventions.html)
(search "Optimistic Locking" on that page — it's a subsection, not a standalone URL).

## 7. Indexes — programmatic, not annotation-based

Indexes in this codebase are **not** `@CompoundIndex` annotations. They're created at startup via
a single `ApplicationRunner` bean in `infrastructure/config/MongoIndexConfig.java`:

```java
Index categoryNameIndex = new Index()
    .on("ownerId", Sort.Direction.ASC)
    .on("name", Sort.Direction.ASC)
    .unique()
    .sparse();
...
mongoTemplate.indexOps("categories").createIndex(categoryNameIndex)
```

Add the two Accounts indexes from `data-model.md` to the same chained `.then(...)` sequence in
that file:
```java
Index accountOwnerIndex = new Index().on("ownerId", Sort.Direction.ASC);
Index accountNameIndex = new Index()
    .on("ownerId", Sort.Direction.ASC)
    .on("name", Sort.Direction.ASC)
    .unique()
    .collation(Collation.of("pt").strength(Strength.SECONDARY));
```
(`Collation.of("pt").strength(Strength.SECONDARY)` gives the `{ locale: "pt", strength: 2 }` the
data model calls for — case/accent-insensitive matching for duplicate-name detection.)

This closes the open checklist item at `docs/architecture-contract.md:422` ("Define the ownerId
indexes per collection in each feature's data-model.md") for the `accounts` collection — the
index definitions already exist in `data-model.md`; this section is just wiring them into
`MongoIndexConfig`.

Docs: [Spring Data MongoDB — Index creation](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/document-indexes.html),
[MongoDB Manual — Collation](https://www.mongodb.com/docs/manual/reference/collation/).

## 8. Command/query handlers — layering example

Full worked example from Category, `CreateCategoryCommandHandler`:
```java
@Component
@RequiredArgsConstructor
public class CreateCategoryCommandHandler {
  private final CategoryRepository categoryRepository;
  private final CategoryReadRepository categoryReadRepository;

  public Mono<Category> handle(CreateCategoryCommand command, String ownerId) {
    // validation against read port, then build + save via write port
  }
}
```
`CreateAccountCommandHandler` is simpler (no parent-lookup validation) — just build the `Account`
domain object with `.ownerId(ownerId).isActive(true)` and call `accountRepository.save(...)`. The
409 (duplicate name) and 422 (validation) responses come for free from `@Valid` on the command
record and the `DuplicateKeyException` mapping in §4 — no extra code needed in the handler itself.

`ReconcileAccountCommandHandler` is the one genuinely new handler shape: it takes `(id, command,
ownerId)`, loads the account via the read port (404 if missing/not owned), then for
`ADJUST_INITIAL_BALANCE` computes the new `initialBalance` from `realBalance` and the current
derived `currentBalance` (needs the §5 aggregation), and calls a repository update. Per the
Deferred-Scope decision (see the conversation's plan), only implement the
`ADJUST_INITIAL_BALANCE` method for now; reject `ADJUSTING_TRANSACTION` with a 422
(`BusinessException`) referencing that it's not yet supported.

## 9. Error responses — nothing new required

Reuse the existing hierarchy as-is: `NotFoundException` → 404, `ConflictException` → 409,
`BusinessException`/`DomainException` → 422, all handled centrally in
`application/GlobalExceptionHandler.java` (`@RestControllerAdvice`). You should not need to add
any new `@ExceptionHandler` methods for Accounts — only new *exception instances* thrown from your
handlers/adapter, using the existing exception classes in `domain/exception/`.

## 10. Testing

Mirror the existing structure:
- **Unit** (`*Test`, no Spring context): `CreateAccountCommandHandlerTest` etc., Mockito-mocked
  ports, `StepVerifier` assertions — same shape as `CreateCategoryCommandHandlerTest`.
- **Integration** (`*IT`): extend `AbstractIntegrationTest` (Testcontainers `MongoDBContainer
  ("mongo:8").withReplicaSet()`, `WebTestClient` on a random port). Add an `AccountSteps` helper
  mirroring `CategorySteps` (wraps `WebTestClient` calls, sets the `accessToken` cookie from
  `UserSteps.createAndGetAccessToken()`).
- **Tenancy test** (mandatory per `api/CLAUDE.md` P2): one cross-tenant isolation test — user A
  cannot `GET`/`PATCH`/`DELETE`/reconcile user B's account (expect 404, not 403).

Docs: [Project Reactor — `StepVerifier`](https://projectreactor.io/docs/test/release/reference/index.html)
(if you haven't used it directly before — this codebase's existing `*Test` classes are the more
directly useful reference).

---

## Summary checklist

- [ ] `Account` domain record + `AccountDocument` (`@Version` included)
- [ ] `AccountMongoRepository` (thin) + `AccountRepositoryAdapter` (owner-scoped queries, duplicate/version error mapping)
- [ ] Indexes added to `MongoIndexConfig` (owner index + unique `{ownerId,name}` with pt collation)
- [ ] CRUD command handlers + controller (`POST/GET/PATCH/DELETE /accounts`)
- [ ] `currentBalance` query handler (aggregation over `transactions`, stub-safe until 2.1)
- [ ] `balance-history` query handler (stub-safe until 2.1)
- [ ] `reconcile` handler — `ADJUST_INITIAL_BALANCE` only, `ADJUSTING_TRANSACTION` rejected with 422
- [ ] Unit tests per handler, integration tests per endpoint, one cross-tenant isolation test
