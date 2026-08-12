# Implementation Notes — Accounts (003)

This file supplements `data-model.md` and `contracts/account.openapi.yaml` with the *why* and
*how* behind each piece, for manual backend implementation. §1–10 cover the original CRUD +
compute-on-read feature (assumes Category, `002-categories`, is already implemented and working).
§11 is a later addendum covering the ADR-003 materialized-balance pivot — it assumes §1–10 and
Transactions (`004-transactions`) are both already implemented and working, since the projector
reads the `transactions` collection those sections/that feature created.

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
infrastructure/adapter/persistence/AccountRepositoryAdapter.java
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

The JWT subject is injected straight into controller methods via a custom `@CurrentOwnerId`
argument annotation (`application/security/CurrentOwnerId.java`), resolved by
`CurrentOwnerIdArgumentResolver` (`infrastructure/config/`), which reads
`ReactiveSecurityContextHolder` and calls `Jwt.getSubject()`:

```java
@GetMapping
public Flux<CategoryResponse> listCategories(@CurrentOwnerId String ownerId) { ... }
```

`ownerId` is then threaded explicitly through every command/query handler call
(`handler.handle(command, ownerId)`), never re-derived from a Reactor `Context` inside the domain
or infrastructure layer. Reproduce this exactly for every `AccountController` endpoint.

The JWT itself arrives via an `accessToken` cookie, not an `Authorization` header — see the
custom `bearerTokenConverter` in `infrastructure/config/SecurityConfig.java`. You don't need to
touch this for Accounts; it's already wired for the whole app.

Docs: [Spring Security reactive OAuth2 resource server — JWT](https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/jwt.html)
(concept: `ReactiveJwtDecoder`, how the `Jwt` principal is built and exposed to
`ReactiveSecurityContextHolder`).

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

### 8.1 `ADJUSTING_TRANSACTION` — resolved design (was deferred, now specified)

Previously this method had no concrete design beyond the 422 stub above. Resolved:

**New factory on `Transaction`:**

```java
// Transaction.java
public static Transaction reconciliation(
    String ownerId, String accountId, TransactionType type, Long amount) {
  return Transaction.builder()
      .ownerId(ownerId)
      .type(type)
      .amount(amount)
      .date(LocalDate.now())
      .description("Reconciliação")
      .accountId(accountId)
      .isReconciliation(true)
      .appliedProjections(Set.of())
      .build();
}
```

Mirrors `accountTransaction(...)`'s shape but has no `CreateTransactionCommand` to read from —
the reconciliation flow is account-initiated, not a normal user-submitted transaction, so the
factory takes its four scalar inputs directly instead of a command object.

**Hexagonal wiring:** `ReconcileAccountCommandHandler` gains a `TransactionRepository` dependency
directly — no new intermediary command/handler needed. This isn't a new pattern for this
codebase: `CreateTransactionCommandHandler` already depends on `AccountReadRepository` for
validation, so a handler in one feature's package depending on another feature's port is already
established practice here, not a hexagonal-boundary violation.

**Behavior:**

```java
// ReconcileAccountCommandHandler.applyReconciliation, ADJUSTING_TRANSACTION branch
return accountReadRepository
    .currentBalance(account.id(), account.ownerId(), account.initialBalance())
    .map(currentBalance -> command.realBalance() - currentBalance)
    .flatMap(delta -> transactionRepository.save(
        Transaction.reconciliation(
            account.ownerId(),
            account.id(),
            delta >= 0 ? TransactionType.CREDIT : TransactionType.DEBIT,
            Math.abs(delta))))
    .thenReturn(account); // unchanged — see below
```

The account itself is **not** saved in this branch (no `accountRepository.save(...)` call). The
new transaction flows through the existing change-stream → SQS →
`ApplyAccountBalanceProjectionCommandHandler` pipeline and updates `currentBalance` asynchronously,
exactly like any other transaction — this is the "no separate handling needed" behavior
`data-model.md`'s reconciliation section already promises. The handler returns the account as it
was fetched at the start of `handle(...)`; the caller sees `currentBalance` catch up on the next
read, same eventual-consistency contract as every other transaction-driven balance change in this
feature.

**Transactional guarantee:** none needed beyond what already exists. This is a single transaction
insert — the same shape as `CreateTransactionCommandHandler`'s SINGLE-mode write — and the
insert→`$inc` atomicity for updating `currentBalance` is already handled inside the projector's
existing `TransactionalOperator` wrap (§11.6). No new multi-document transaction is required in
`ReconcileAccountCommandHandler` itself.

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

## 11. Materialized `currentBalance` (ADR-003) — supersedes §5's compute-on-read model

Since §5 was written, Transactions (004) has shipped and the architecture pivoted:
`docs/adr/ADR-003-materialized-derived-balances.md` supersedes contract P4's "computed, never
stored" rule for `currentBalance`. This section replaces §5's `currentBalance` guidance —
`balance-history`'s day-by-day sparkline logic is a separate query and is only lightly touched
(§11.8). Read `docs/technical-solutions/materialized-projections.md` (SPEC-CROSS-01) first — it
has the full architecture diagram and sequence diagrams this section turns into code, scoped here
to the account-balance half only (invoice totals are `005-cards`'s job, not built yet).

**What changes vs §5:** `currentBalance` becomes a stored field on `Account`/`AccountDocument`
instead of a per-request aggregation. Two new infrastructure components keep it in sync: a
MongoDB Change Stream listener (the trigger — catches every `transactions` write, so no code path
can forget to fire it) and an SQS `@SqsListener` consumer (the effect — applies the `$inc`),
reusing the exact queue/DLQ shape `ADR-002` already designed for invoice generation.

### 11.0 Build order — work in these slices, not all at once

The sections below are organized by *concern* (schema, trigger, consumer, ...), not by the order
you should build them in. Build in this order instead — each slice is independently mergeable and
testable without the next one existing yet, so you get working checkpoints instead of one large
change that only compiles at the very end:

1. **SQS plumbing only, no business logic (§11.3).** Add the dependency, flip
   `SERVICES=secretsmanager` → `secretsmanager,sqs` in `docker-compose.yml`, write
   `02-seed-sqs.sh`. Prove it works with a throwaway `@SqsListener` that just logs what it
   receives and a test that sends one message and asserts it arrived. Delete the throwaway once
   you're confident — the point of this slice is isolating "does the queue exist and round-trip"
   from everything else, before any balance logic exists to confuse a failure with.
2. **Schema additions, no behavior change (§11.1–§11.2).** Add `Account.currentBalance` /
   `Transaction.appliedProjections` and their document fields, defaulted so nothing currently
   reads them yet. Every existing test should still pass untouched — this is a safe, mechanical
   step, and a good place to stop and confirm nothing regressed before moving on.
3. **The trigger alone (§11.4–§11.5's listener half).** Build
   `AccountBalanceChangeStreamListener` publishing `BalanceProjectionMessage`s to the now-real
   queue, plus `ProjectionCheckpointDocument`. Nothing consumes them yet — verify via test that
   creating a transaction produces a message on the queue, not that any balance changes (it won't
   yet).
4. **The consumer (§11.6).** Build `AccountBalanceProjector`, the idempotency guard, and the
   `TransactionalOperator`-wrapped `$inc`. This is where balances actually start updating for the
   first time. Write the idempotency-guard test here — it's the single most valuable test in this
   whole section, and the easiest to write in isolation (bypass the queue, call the listener
   method directly twice, assert the balance moved once).
5. **Turn it on for real (§11.5's backfill half + §11.7 + §11.9).** Run the one-time backfill —
   or skip it if the environment has no pre-existing data (see §11.5's note) — then flip the six
   `AccountController` call sites and fix `ReconcileAccountCommandHandler` in the same slice,
   since they're small and tightly related — this is the slice where `currentBalance` becomes
   real for every account for the first time.
6. **`balance-history` (§11.8)** — independent of everything above (it never reads
   `currentBalance`), so it's fine to do it any time after step 2, whenever's convenient. Not a
   blocking dependency of steps 3–5 or vice versa.
7. **Reconciliation job / snapshot scheduler (§11.10)** — explicitly deferred, separate future
   work once the above is stable in practice, not blocking anything above.

### 11.1 Domain model changes

```java
// Account.java — add one field
public record Account(
    String id, String ownerId, String name, AccountType type, String institution,
    Long initialBalance,
    Long currentBalance,   // NEW — materialized, mutated only via reconcileBalances() below
    String color, Boolean isActive, Instant createdAt, Long version) { ... }

// Transaction.java — add one field, defaulted to empty on every factory method
public record Transaction(
    ..., Boolean isReconciliation,
    Set<String> appliedProjections,   // NEW — idempotency guard, defaults to Set.of()
    Instant createdAt, Instant updatedAt, Long version) { ... }
```

Set `appliedProjections(Set.of())` in `Transaction.accountTransaction`/`debitCardTransaction`/
`creditCardTransaction` (mirrors how `tags`/`notes` are already threaded through those factories).
`CreateAccountCommandHandler` should set `currentBalance(command.initialBalance())` at creation —
a brand-new account's current balance starts equal to its initial balance, same invariant §5's
formula already states (`initialBalance + 0 - 0`).

### 11.2 Persistence additions

- `AccountDocument`: add `private Long currentBalance;`.
- `TransactionDocument`: add `private Set<String> appliedProjections;` (same `Set<String>` shape
  as the existing `tags` field, not `List` — order never matters for a membership check).
- New `ProjectionCheckpointDocument` (`@Document(collection = "projection_checkpoints")`, own
  Mongo repository): `@Id private String projectionName;` (e.g. `"accountBalance"`),
  `private Document resumeToken;` (`org.bson.Document` — the raw Mongo change-stream resume
  token, opaque, don't try to model its internal shape), `private String
  lastProcessedTransactionId;`, `private Instant updatedAt;`. This doesn't extend `BaseDocument`
  — it has no `ownerId` (infra-only shared state, same reasoning as `shedLock` in ADR-002) and no
  `_version` (single writer: the listener itself, never contended).
- New `BalanceSnapshotDocument` (`@Document(collection = "balance_snapshots")`): `ownerId`,
  `accountId`, `asOfDate` (`LocalDate`), `balance` (`Long`), `lastTransactionId`. Not built by
  this pass (§11.10), but the schema is here so the backfill/reconciliation follow-up doesn't
  need a second data-model pass.

### 11.3 Standing up SQS — new ground, not just for Accounts

**`ADR-002` designed this infra for `005-cards`, but `005-cards` hasn't been built yet — this is
actually the first feature in the codebase to stand up SQS.** Whichever of the two features lands
first does this work; the other reuses it. Concretely, today only
`spring-cloud-aws-starter-secrets-manager` exists and LocalStack only runs `secretsmanager`
(`docker-compose.yml`). You'll need to add:

- `libs.versions.toml`: `spring-cloud-aws-sqs = { module = "io.awspring.cloud:spring-cloud-aws-starter-sqs", version.ref = "spring-cloud-aws" }`
  (same `spring-cloud-aws` version ref the secrets-manager starter already uses).
- `api/build.gradle`: `implementation libs.spring.cloud.aws.sqs`.
- `docker-compose.yml`: change `SERVICES=secretsmanager` to `SERVICES=secretsmanager,sqs`.
- `localstack/init/02-seed-sqs.sh` (numbered after the existing `01-seed-secrets.sh`, same
  `awslocal` pattern):
  ```bash
  #!/bin/bash
  set -e

  awslocal sqs create-queue --queue-name mithril-vault-balance-projection-dlq
  DLQ_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/mithril-vault-balance-projection-dlq \
    --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

  awslocal sqs create-queue --queue-name mithril-vault-balance-projection \
    --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}"
  ```
- No ShedLock needed for this part — the change-stream listener and the `@SqsListener` consumer
  aren't `@Scheduled` jobs contending across instances the way the deferred snapshot/reconciliation
  jobs would be (§11.10). Don't add a distributed lock here; there's nothing to coordinate.

Docs: [Spring Cloud AWS — SQS Integration](https://docs.awspring.io/spring-cloud-aws/docs/current/reference/html/index.html#sqs-integration)
(concept: `@SqsListener`, `SqsTemplate`).

### 11.4 `AccountBalanceChangeStreamListener` (the trigger)

Lives in `infrastructure/adapter/projection/` (new package — nothing currently there). Opens a
reactive Mongo change stream on `transactions` and publishes one `BalanceProjectionMessage` per
insert:

```java
public record BalanceProjectionMessage(
    String ownerId, String transactionId, String accountId, String invoiceId,
    String type, Long amount, String target) {} // target: "ACCOUNT" | "INVOICE"
```

```java
reactiveMongoTemplate.changeStream(TransactionDocument.class)
    .watchCollection("transactions")
    .filter(where("operationType").is("insert"))
    .resumeAt(checkpointRepository.findById("accountBalance")
        .map(ProjectionCheckpointDocument::getResumeToken))   // empty on first-ever boot
    .listen()
    .flatMap(event -> {
      var txn = event.getBody();
      var message = new BalanceProjectionMessage(txn.getOwnerId(), txn.getId(),
          txn.getAccountId(), txn.getInvoiceId(), txn.getType().name(), txn.getAmount(),
          txn.getAccountId() != null ? "ACCOUNT" : "INVOICE");
      return sqsTemplate.send("mithril-vault-balance-projection", message)
          .then(checkpointRepository.save(new ProjectionCheckpointDocument(
              "accountBalance", event.getResumeToken(), txn.getId(), Instant.now())));
    })
    .subscribe();
```

(Sketch, not literal — check the exact reactive change-stream builder API on the Spring Data
MongoDB version this project pins; the shape above is the general contract:
`ReactiveMongoOperations.changeStream(Class)` returning a fluent builder with
`.watchCollection(String)`, `.filter(Criteria)`, `.resumeAt(...)`, `.listen()` returning
`Flux<ChangeStreamEvent<T>>`.) Since this account-balance pass only cares about `ACCOUNT`-target
transactions today (§11.6 defers invoices), you can either publish `INVOICE`-target messages too
(harmless — no consumer reads them until `005-cards` exists) or filter them out client-side; either
is fine, don't over-think it.

Docs: [Spring Data MongoDB — Change Streams](https://docs.spring.io/spring-data/mongodb/reference/mongodb/reactive-change-streams.html).

### 11.5 One-time backfill — ordering matters

Run **before** the listener starts consuming, or accounts double-count every transaction that
existed before backfill ran:

1. For every existing account, compute `recomputeBalance` (§5's original aggregation, kept as-is
   just renamed) and set `currentBalance` to that value.
2. Only then start `AccountBalanceChangeStreamListener` — and start it **from "now"**, not from
   the beginning of the collection (an empty/absent checkpoint should resume from the current
   change-stream position, not replay history) — otherwise every transaction the backfill already
   accounted for gets `$inc`'d a second time.

A simple `ApplicationRunner` gated by a one-off property (or a manual script run once against
prod data) is enough — this isn't a repeatable migration, it runs exactly once per environment.

**No-op on a fresh environment:** this step only matters if `accounts`/`transactions` already
have data before the listener starts — there's nothing to backfill against an empty database.
Wiping the local Mongo volume and starting clean (accounts created after this point already get
`currentBalance = initialBalance` per §11.1, and the listener starts "from now" with no history
to double-count) makes this step a no-op; skip the runner/script entirely in that case. Only
build it when there's real pre-existing data to reconcile — a populated staging/prod environment,
not local dev.

### 11.6 `AccountBalanceProjector` (`@SqsListener` consumer) — the atomicity gap, and closing it

**Implemented as:** `ApplyAccountBalanceProjectionCommandHandler`, not a class literally named
`AccountBalanceProjector`. It also goes through a `ProjectionRepository` port
(`ProjectionCheckpointRepositoryAdapter` infra implementation) rather than calling
`ReactiveMongoTemplate`/`TransactionalOperator` inline as sketched below — the sketch's behavior
is what was built, just behind an extra port/adapter layer for the checkpoint state.

```java
@SqsListener("mithril-vault-balance-projection")
public Mono<Void> handle(BalanceProjectionMessage message) {
  if (!"ACCOUNT".equals(message.target())) {
    return Mono.empty(); // INVOICE target — not this feature's job, see §11.3
  }
  long signedAmount = "CREDIT".equals(message.type()) ? message.amount() : -message.amount();

  return transactionalOperator.execute(status ->
      markApplied(message.transactionId())
          .flatMap(applied -> applied
              ? accountMongoTemplate.updateFirst(
                  Query.query(Criteria.where("_id").is(message.accountId())),
                  new Update().inc("currentBalance", signedAmount),
                  AccountDocument.class)
              : Mono.empty())
  ).then();
}
```

**A real design gap worth naming explicitly, not glossing over:** ADR-003's prose describes
"a single `findOneAndUpdate`" for the idempotency guard, but the guard lives on the `transactions`
document and the effect (`$inc`) lands on a *different* document in a *different* collection
(`accounts`) — there is no single native Mongo operation spanning both. Two separate writes means
a crash between them is possible: mark `appliedProjections` first, then `$inc` — if the process
dies in between, the transaction is marked "already applied" and a redelivered message becomes a
silent no-op, permanently undercounting that one transaction, with the reconciliation job (§11.10,
**explicitly deferred in this pass**) as the only thing that would ever notice.

**Close it instead of accepting it:** this codebase already has a `TransactionalOperator` bean
(`infrastructure/config/MongoTransactionConfig.java`, used today by `CreateTransferCommandHandler`
for its two-leg write — same tool, same replica set, same reactive transaction manager). Wrap the
mark-applied write and the `$inc` in one Mongo multi-document transaction, same pattern as the
transfer handler: either both happen or neither does, closing the crash window entirely without
needing the reconciliation job as a safety net for *this specific* failure mode. Reconciliation is
still worth building eventually (§11.10) — it catches other drift classes, like a genuine logic
bug in this method — but it shouldn't be the only thing standing between a crash and a silently
wrong balance when a five-line transactional wrap removes the gap outright.

`markApplied` is the guard itself:

```java
private Mono<Boolean> markApplied(String transactionId) {
  return transactionMongoTemplate.findAndModify(
      Query.query(Criteria.where("_id").is(transactionId)
          .and("appliedProjections").ne("accountBalance")),
      new Update().addToSet("appliedProjections", "accountBalance"),
      TransactionDocument.class)
    .map(doc -> true)   // matched → wasn't applied before, we just marked it → apply the $inc
    .defaultIfEmpty(false); // no match → already applied → redelivery/replay no-op
}
```

Docs: [Spring Data MongoDB — Reactive Transactions](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-transactions.html)
(same doc `implementation-notes.md` for `004-transactions` §5 already links — you're reusing the
exact same mechanism, not learning a new one).

### 11.7 Read-path rewire

**Status: controller half done, port half still open.**

Done: the six `AccountController` call sites that used to pass `account.initialBalance()` as the
`currentBalance` argument now use the account's own materialized `currentBalance` — `create`,
`update`, `reactivate`, `get`, `list`, `reconcile`. Every one of them was a placeholder (there was
never a working aggregation wired into the controller before this). The mapper itself was
simplified to `accountResponseMapper.toResponse(account)` (single-arg, MapStruct maps
`currentBalance` straight off the domain record) rather than taking an explicit second
`currentBalance` parameter — cleaner than the original two-arg sketch above, since the domain
model now always carries the field itself.

Still open: `AccountReadRepository.currentBalance` (`AccountRepositoryAdapter`) is still the §5
aggregation, not a plain field read. `ReconcileAccountCommandHandler` still calls it to compute
the reconciliation delta (recomputing truth from `transactions` is actually the safer source for
that specific use, so leaving it as an aggregation there isn't wrong) — but per the original plan
here, this method was meant to become a plain field read once the projector is trustworthy, with
the aggregation renamed `recomputeBalance` and reserved for the backfill (§11.5) and the future
reconciliation job (§11.10). Neither the rename nor the backfill has been done yet.

### 11.8 `balance-history` — implement for real now that `transactions` exists

§5 left this stubbed ("return `initialBalance` for all 30 days") because `transactions` didn't
exist yet. It does now (004 shipped) — implement the pipeline §5 already sketched: `$match` the
account + a 30-day window, `$group by day` summing signed amounts, then a running cumulative sum
in the reactive chain (`Flux.scan`, seeded with the balance as of 30 days ago — **not**
`currentBalance**, since that's today's value; you need the balance at the *start* of the window,
which itself requires either a `recomputeBalance`-style scoped aggregation for "as of 30 days ago"
or, more simply, computing `currentBalance - netSumOfLast30Days` as the seed). This query is
independent of the materialization change — it's not reading `accounts.currentBalance` at all,
it's a historical series over `transactions` — so it isn't blocked by anything in this section,
just no longer blocked by "transactions doesn't exist" either. Leave
`// TODO(ADR-003): bound this via balance_snapshots once the reconciliation job exists` since
right now it's still an unbounded scan over up to 30 days of transactions (bounded enough to not
matter yet, unlike the old full-history `currentBalance` aggregation this ADR replaces).

### 11.9 `ReconcileAccountCommandHandler` — must set both fields together

Per `specs/003-accounts/data-model.md`'s "Direct adjustment path," this is the one application-code
write allowed to touch `currentBalance` directly (alongside the projector). Today's
`applyReconciliation` only calls `account.reconcileInitialBalance(...)`. Replace with a method
that sets both:

```java
// Account.java
public Account reconcileBalances(Long newInitialBalance, Long newCurrentBalance) {
  return this.toBuilder()
      .initialBalance(newInitialBalance)
      .currentBalance(newCurrentBalance)
      .build();
}
```

```java
// ReconcileAccountCommandHandler.applyReconciliation
return accountReadRepository
    .currentBalance(account.id(), account.ownerId(), account.initialBalance()) // still fine to
    // call this — see note below
    .map(currentBalance -> account.reconcileBalances(
        account.initialBalance() + (command.realBalance() - currentBalance),
        command.realBalance()));
```

Note: `AccountReadRepository.currentBalance(...)` at this call site should keep reading the
*materialized* field once §11.7 lands (it's the same port method, just backed by a field read
instead of an aggregation now) — the reconcile flow doesn't need any code change here beyond
`reconcileBalances`, it already goes through the port.

### 11.10 Explicitly deferred in this pass

- `BalanceSnapshotScheduler` / `balance_snapshots` population — schema exists (§11.2), nothing
  writes to it yet. Add `// TODO(ADR-003): BalanceSnapshotScheduler, see materialized-projections.md §3.3`.
- `BalanceReconciliationJob` — not built. Add `// TODO(ADR-003): reconciliation job, see
  materialized-projections.md §3.4` near `recomputeBalance`. §11.6 above explains exactly which
  failure mode this leaves open and why the transactional wrap already closes the worst of it.
- `InvoiceTotalProjector`, `Card`/`Invoice` materialization — `005-cards`'s job, not this pass.
- Budget `spentAmount` / dashboard "Saldo Líquido" reusing this pattern — future work once those
  features exist.

### 11.11 Indexes

Add via the same chained `MongoIndexConfig` pattern as §7:

```java
Index balanceSnapshotLookupIndex = new Index()
    .on("ownerId", Sort.Direction.ASC)
    .on("accountId", Sort.Direction.ASC)
    .on("asOfDate", Sort.Direction.DESC);
mongoTemplate.indexOps("balance_snapshots").createIndex(balanceSnapshotLookupIndex)
```

No index needed on `accounts.currentBalance` itself (never queried/filtered on) or on
`transactions.appliedProjections` (the idempotency guard's `findAndModify` always filters on `_id`
first, which is already the primary key — `appliedProjections` only narrows within that single
document, not a separate index scan).

### 11.12 Testing

- **Unit**: test `markApplied`'s intent directly is hard without Mongo (it's a `findAndModify`
  semantics test) — push that coverage into the integration test instead. Do unit-test
  `Account.reconcileBalances` (pure), and the `BalanceProjectionMessage` construction logic in the
  change-stream listener if you factor it into a small pure function.
- **Integration** (`*IT`, Testcontainers — `AbstractIntegrationTest` already runs Mongo as a
  replica set, required for change streams; you'll also need a `LocalStackContainer` Testcontainers
  module now, new to this codebase's tests — flag this as shared test infra `005-cards` will also
  want once it's built, so build it once, cleanly, here): create a transaction via the existing
  `TransactionSteps.create(...)`, then poll `GET /accounts/{id}` (a short retry/await loop, not a
  fixed `Thread.sleep`) until `currentBalance` reflects it — this is now a genuinely
  eventually-consistent read, the test has to treat it as one. A second test: publish the same
  `BalanceProjectionMessage` twice directly against the listener (bypassing the full pipeline) and
  assert `currentBalance` only moved once — this is the actual idempotency-guard regression test
  and doesn't need real SQS redelivery to prove the guard works.

Docs: [Testcontainers — LocalStack module](https://testcontainers.com/modules/localstack/).

---

## Summary checklist — Part 1: CRUD + compute-on-read (§1–10, already shipped)

- [x] `Account` domain record + `AccountDocument` (`@Version` included)
- [x] `AccountMongoRepository` (thin) + `AccountRepositoryAdapter` (owner-scoped queries, duplicate/version error mapping)
- [x] Indexes added to `MongoIndexConfig` (owner index + unique `{ownerId,name}` with pt collation)
- [x] CRUD command handlers + controller (`POST/GET/PATCH/DELETE /accounts`)
- [x] `currentBalance` query handler (aggregation over `transactions`) — superseded by Part 2 below
- [x] `balance-history` query handler (flat-line stub) — real implementation is Part 2, §11.8
- [x] `reconcile` handler — `ADJUST_INITIAL_BALANCE` only, `ADJUSTING_TRANSACTION` rejected with 422
- [x] Unit tests per handler, integration tests per endpoint, one cross-tenant isolation test

## Summary checklist — Part 2: materialized `currentBalance` (§11, ADR-003)

Ordered per §11.0's build order — each numbered group is a mergeable slice, not a grab-bag:

**1. SQS plumbing (§11.3)**
- [x] `spring-cloud-aws-starter-sqs` dependency added
- [x] LocalStack `SERVICES` includes `sqs`; `02-seed-sqs.sh` provisions the queue + DLQ
- [x] Round-trip proven — `BalanceProjectionQueuePublisherIT`, `BalanceProjectionListenerIT`

**2. Schema additions, no behavior change (§11.1–11.2)**
- [x] `Account.currentBalance` + `AccountDocument.currentBalance` fields
- [x] `Transaction.appliedProjections` + `TransactionDocument.appliedProjections` fields
- [x] `ProjectionCheckpointDocument` + `BalanceSnapshotDocument` (schema only, unused so far)
- [x] Full existing test suite still green

**3. Trigger alone (§11.4)**
- [x] `AccountBalanceChangeStreamListener` — change stream trigger, publishes to SQS, checkpoints
- [x] Test: `AccountBalanceChangeStreamListenerIT` — publishes + advances checkpoint per inserted transaction, survives a failed event

**4. Consumer (§11.6)**
- [x] Implemented as `ApplyAccountBalanceProjectionCommandHandler` (`@SqsListener`), not literally
      named `AccountBalanceProjector` — idempotency guard + `$inc`, wrapped in one Mongo
      transaction via the existing `TransactionalOperator`, behind a `ProjectionRepository` port
- [x] Idempotency-guard test: `ApplyAccountBalanceProjectionCommandHandlerTest`
- [x] **Bug fixed 2026-08-02:** the `@SqsListener` method (`BalanceProjectionListener.handle`) was
      returning `Mono<Void>`. Spring Cloud AWS SQS 4.0.0-RC1 has no Reactor support — its listener
      adapter casts the return value directly to `CompletableFuture<Void>`, so the `Mono` was never
      subscribed to and the projection silently never ran, even though the message still
      acknowledged as if it had. Changed the method to return `CompletableFuture<Void>` (via
      `.toFuture()`) instead. This was only caught by adding a real end-to-end test through the
      actual `@SqsListener` container (see slice 5's new integration test below) — every prior test
      either mocked the queue publisher or called `listener.handle(...)` directly, bypassing the
      real container's return-type handling entirely.

**5. Turn it on for real (§11.5, §11.7, §11.9)**
- [ ] One-time backfill run, listener started from "now" (not replayed from the beginning) — not done
- [x] Read-path rewire — all six `AccountController` call sites switch to `account.currentBalance()`
      (fixed 2026-08-02; mapper simplified to single-arg `toResponse(account)`)
- [x] `ReconcileAccountCommandHandler` sets `initialBalance` + `currentBalance` together via
      `Account.reconcileBalances` (fixed 2026-08-02; a follow-up bug where the reconciled result
      was computed but discarded — the handler returned the original, unreconciled account — was
      also fixed 2026-08-02)
- [x] `AccountReadRepository.currentBalance` — resolved by removal rather than rename: nothing
      calls it anymore (every read path now uses the account's own materialized `currentBalance`
      field, including `ReconcileAccountCommandHandler`), so there's no aggregation left to rename
      to `recomputeBalance` (fixed 2026-08-02)
- [x] `balance_snapshots` index added to `MongoIndexConfig` (§11.11) — done
- [x] Integration test: end-to-end eventual-consistency convergence — `AccountBalanceEventualConsistencyIT`
      (fixed 2026-08-02), creates a transaction via the real HTTP API and polls `GET /accounts/{id}`
      until `currentBalance` reflects it, through the real change-stream → SQS → `@SqsListener`
      pipeline (LocalStack, no mocks). This is what caught the `Mono`/`CompletableFuture` bug above.

**6. `balance-history` (§11.8)**
- [x] Implemented for real (fixed 2026-08-02): `$match`-equivalent query over `transactions` for the
      account + 30-day window, grouped by day in Java, seeded from `currentBalance -
      netChangeOverWindow` and run forward via `Flux.scan`. Left as
      `// TODO(ADR-003): bound this via balance_snapshots once the reconciliation job exists` per
      the note below — still an unbounded scan over the window, acceptable at this data volume.

**7. Deferred, not built this pass (§11.10)**
- [ ] `BalanceSnapshotScheduler`, `BalanceReconciliationJob` — not built (by design, deferred)
