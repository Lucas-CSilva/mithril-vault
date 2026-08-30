# Implementation Notes — Transactions (004)

This file supplements `data-model.md` and `contracts/transaction.openapi.yaml` with the *why* and
*how* behind each piece, for manual backend implementation. It assumes you already have the
Accounts feature (`003-accounts`) implemented and working — sections that are identical to 003
are called out tersely ("same as 003") rather than re-explained; only what's genuinely new to
Transactions gets full treatment.

Stack versions for the doc links: **Spring Boot 4.0.6** (BOM-managed — Spring Framework 7.0.x,
Spring Data MongoDB 5.x, Spring Security 6.x/7.x pulled in via the BOM).

---

## 1. Where this feature lives (hexagonal layering)

```
domain/model/Transaction.java                      — plain record, no persistence annotations
domain/model/RecurringTransactionSeries.java        — series template, not an instance (ADR-005)
domain/model/{PaymentMethod,TransactionType,ImportSource,SourceType}.java  — enums
domain/command/transaction/CreateTransactionCommand.java   — carries mode (SINGLE/RECURRING/INSTALLMENT/TRANSFER)
domain/command/transaction/UpdateTransactionCommand.java    — description/categoryId/notes/tags only
domain/commandhandler/transaction/CreateTransactionCommandHandler.java
domain/commandhandler/transaction/CreateRecurringTransactionCommandHandler.java
domain/commandhandler/transaction/CreateInstallmentCommandHandler.java
domain/commandhandler/transaction/CreateTransferCommandHandler.java
domain/commandhandler/transaction/UpdateTransactionCommandHandler.java
domain/queryhandler/transaction/ListTransactionsQueryHandler.java
domain/queryhandler/transaction/GetTransactionQueryHandler.java
domain/service/CategorySuggestionService.java       — keyword matching, no ports needed
domain/port/{TransactionRepository,TransactionReadRepository}.java
domain/port/{RecurringSeriesRepository,RecurringSeriesReadRepository}.java
application/controller/TransactionController.java   — no DELETE route (ADR-005)
application/response/TransactionResponse.java (+ TransactionPageResponse)
application/mapper/TransactionResponseMapper.java
infrastructure/persistence/document/TransactionDocument.java
infrastructure/persistence/document/RecurringTransactionSeriesDocument.java
infrastructure/persistence/TransactionMongoRepository.java
infrastructure/adapter/persistence/TransactionRepositoryAdapter.java
infrastructure/mapper/TransactionMapper.java (MapStruct, domain ↔ document)
infrastructure/scheduler/RecurringTransactionGenerationJob.java (ADR-005)
```

`CreateTransactionCommand`'s `mode` field is the dispatch key: the controller's `create` method
reads `command.mode()` and routes to the matching command handler (a small `switch`, or a
strategy map keyed by `TransactionMode` — either is fine, don't over-engineer a plugin registry
for four cases).

Reference: `api/CLAUDE.md` hexagonal diagram — domain has zero Spring/Mongo imports; only
`application` and `infrastructure` know about either.

## 2. Domain model vs. persistence document

Same split as `Account`/`AccountDocument`: `Transaction` is a plain Lombok `@Builder` record;
`TransactionDocument extends BaseDocument` carries `@Document(collection = "transactions")`,
`@Getter/@Setter/@SuperBuilder/@FieldNameConstants`, plus `@Version private Long version;`
(concurrent edits are possible here too — recurring-series regeneration rewrites future instances
while a user could be editing one).

**New ground — the account/invoice XOR constraint.** `data-model.md` states: *"Exactly one of
`accountId` or `invoiceId` must be non-null. Enforced at the domain layer."* This is **not** a
Mongo schema validator and not a Jakarta Bean Validation annotation on the command (you can't
express XOR-across-two-nullable-fields cleanly with `@NotNull` combinations without a custom
constraint). Enforce it explicitly in the command handler before building the domain object:

```java
if ((command.accountId() == null) == (command.invoiceId() == null)) {
  throw new BusinessException(ErrorCode.VALIDATION_FAILED,
      "Exactly one of accountId or invoiceId must be set");
}
```

(`ErrorCode` currently has no transaction-specific value — reuse `VALIDATION_FAILED`, same as any
other 422 in this codebase; don't add a new enum constant just for this one message.)

Same rule applies to the `creditCardId` alternative in `CreateTransactionCommand` — if present,
resolve it to the correct open `invoiceId` (based on the card's `closingDay` vs. the transaction
`date`, per `docs/product-definition.md` §Module 2) *before* this check, so by the time the
check runs there's always exactly one of `accountId`/`invoiceId` populated.

`TransactionMapper` (MapStruct) does document ↔ domain, same shape as `AccountMapper`.
`TransactionResponseMapper` (application layer) does domain → `TransactionResponse`, dropping
`ownerId` per the "least disclosure" rule — note the OpenAPI schema explicitly documents this
("Does not include ownerId").

## 3. Tenancy — where `ownerId` comes from

Same as 003, now via the `@CurrentOwnerId` argument annotation
(`application/security/CurrentOwnerId.java`, resolved by
`infrastructure/config/CurrentOwnerIdArgumentResolver.java`) instead of the old
`@AuthenticationPrincipal(expression = "subject")` SpEL pattern — that TODO from
`docs/architecture-contract.md` §8 was closed before this feature started. Use
`@CurrentOwnerId String ownerId` on every `TransactionController` endpoint and thread it
explicitly through every handler call, exactly as `AccountController` does.

## 4. Repository & queries

Same adapter pattern as Accounts: `TransactionMongoRepository` is a thin
`ReactiveMongoRepository<TransactionDocument, String>` marker; all actual filtering happens in
`TransactionRepositoryAdapter` via `ReactiveMongoTemplate` + `Query`/`Criteria`.

**New ground — `ListTransactionsQuery` has the richest filter set in the codebase so far.** Every
filter in the OpenAPI `GET /transactions` params (`accountId`, `invoiceId`, `categoryId`, `type`,
`paymentMethod`, `startDate`/`endDate`, `search`) is optional and additive — build the `Criteria`
incrementally:

```java
Criteria criteria = Criteria.where(TransactionDocument.Fields.ownerId).is(ownerId);
if (query.accountId() != null) criteria.and(TransactionDocument.Fields.accountId).is(query.accountId());
if (query.startDate() != null || query.endDate() != null) {
  criteria.and(TransactionDocument.Fields.date).gte(query.startDate()).lte(query.endDate());
}
if (query.search() != null) {
  criteria.and(TransactionDocument.Fields.description)
      .regex(Pattern.quote(query.search()), "i");
}
```

Pagination (`page`/`size`) + sort (`date` descending, per contract) uses Spring Data's
`PageRequest`/`Query.with(pageable)`, returning `Mono<Page<Transaction>>` mapped into the
`TransactionPage`/`TransactionPageResponse` shape (`content`, `page`, `size`, `totalElements`,
`totalPages`) — the first paginated list endpoint in this codebase; `Account`'s `list` returns a
plain `Flux`.

Duplicate-key mapping (importHash/fitid unique-sparse indexes, §9) uses the same
`DuplicateKeyException → ConflictException` pattern as `AccountRepositoryAdapter.save()`, but here
a duplicate is an **expected, silent** outcome for imports (dedup), not necessarily an error to
surface — see §7 (deduplication) below for how the import path should treat it.

Docs: [Spring Data MongoDB — Querying Documents](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-query-operations.html),
[Paging and Sorting](https://docs.spring.io/spring-data/mongodb/reference/repositories/core-concepts.html#repositories.core-concepts.paging).

## 5. New ground — atomic transfers (P7)

**Nothing in the codebase runs a real multi-document Mongo transaction yet** — the
`ReactiveMongoTransactionManager` bean already exists
(`infrastructure/config/MongoTransactionConfig.java`) and the replica set is already enabled
(contract §8), but no command handler uses it. `CreateTransferCommandHandler` is the first.

A transfer writes two `Transaction` documents — a DEBIT from the source account and a CREDIT to
the destination account, both carrying the same `transferPairId` — and per contract P7 this
**MUST** be transactional: either both legs persist or neither does. Wrap the two saves in a
`TransactionalOperator`:

```java
@RequiredArgsConstructor
public class CreateTransferCommandHandler {
  private final TransactionRepository transactionRepository;
  private final TransactionalOperator transactionalOperator;

  public Flux<Transaction> handle(CreateTransactionCommand command, String ownerId) {
    String pairId = command.transfer().transferPairId() != null
        ? command.transfer().transferPairId()
        : UUID.randomUUID().toString();

    Transaction debitLeg = /* build DEBIT leg, accountId = command.accountId() */;
    Transaction creditLeg = /* build CREDIT leg, accountId = command.transfer().destinationAccountId() */;

    return transactionRepository.saveAll(List.of(debitLeg, creditLeg))
        .as(transactionalOperator::transactional);
  }
}
```

`TransactionalOperator` is injected as a bean (Spring Boot auto-configures one once
`ReactiveMongoTransactionManager` exists), so no new config is needed beyond what
`MongoTransactionConfig` already provides.

**Idempotency.** Per `data-model.md`'s transfer rule, re-submitting the same `transferPairId` is a
no-op — before writing, check `transactionReadRepository.existsByTransferPairId(ownerId,
transferPairId)` and short-circuit (return the existing pair) rather than erroring. This is why
`transferPairId` has its own sparse index (§9) — it's the transfer's natural idempotency key, same
concept as `importHash`/`fitid` for imports.

**Exclusion from KPIs.** Transfers must be excluded from income/expense totals wherever those are
computed later (budgets, dashboards) — nothing to implement here, just don't forget it exists as a
constraint when those features land; `paymentMethod = TRANSFER` is the marker to filter on.

Docs: [Spring Data MongoDB — Reactive Transactions](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-transactions.html)
(concept: `ReactiveMongoTransactionManager`, `TransactionalOperator`, requires replica set — already
satisfied), [MongoDB Manual — Transactions](https://www.mongodb.com/docs/manual/core/transactions/).

## 6. New ground — recurring & installment generation

`CreateRecurringTransactionCommandHandler` and `CreateInstallmentCommandHandler` no longer share
the same shape they once did — see
`docs/adr/ADR-005-transaction-immutability-and-deferred-recurring-generation.md` for the full
reasoning. Full task breakdown: `tasks/003-recurring-series-and-instance.md`,
`tasks/004-recurring-generation-job.md`, `tasks/005-installment.md`.

- **Installment** (unchanged from the original design): `installment.totalInstallments` (N)
  transactions, `amount = totalAmount / N` (integer division — **money rule**: never introduce a
  float here), with the remainder centavo added to installment 1 (`data-model.md` business rule).
  Each installment is assigned to the corresponding *monthly* invoice — installment *k* goes to
  the invoice *k* months after the first one's invoice, following the same closing-day logic as
  §2's `creditCardId` resolution. All N are saved via `transactionRepository.saveAll(...)` at
  creation time (no transaction wrapper needed — a partial write is an incomplete, regenerable
  series, not a correctness problem). Installments only apply to credit-card transactions.
- **Recurring** (changed — split into immediate + deferred): `CreateRecurringTransactionCommandHandler`
  inserts *only* the instance(s) due today or earlier (in practice: the single instance dated
  `command.date()`, if it's `<= today`), plus a new `RecurringTransactionSeries` document
  (`recurring_transaction_series` collection) carrying the series template and
  `nextOccurrenceDate`. It does **not** generate the rest of the horizon. A new
  `RecurringTransactionGenerationJob` (`@Scheduled`, `@DistributedLock`-guarded, same shape as
  `BalanceReconciliationJob`/`BalanceSnapshotJob`) generates each remaining instance as its date
  arrives, advancing `nextOccurrenceDate` by `frequency` each time, stopping once
  `nextOccurrenceDate > endDate` (if set).

  Why: `AccountBalanceProjector` applies `$inc` unconditionally at insert time with no date check
  — eagerly inserting a future-dated instance would immediately (and wrongly) change today's
  materialized `accounts.currentBalance`. Installments don't have this problem because they target
  `invoiceId`, and a future invoice's `totalAmount` is *supposed* to show known future charges.

**Editing and deleting.** Per ADR-005, transactions are append-only: no `editScope`, no
`deleteScope`, no "this and all future" concept, and no delete endpoint at all. See §11 (updated)
and `tasks/006-update-restricted.md`.

## 7. New ground — deduplication (import dedup keys)

Not exercised by the manual-entry endpoints in this feature's OpenAPI contract (import itself is
`005-import`/`006` per the implementation plan), but the `importHash`/`fitid` fields and their
unique-sparse indexes belong to this collection now, so wire them at the document/index level in
this feature even though no import command uses them yet:

- CSV: `importHash = SHA-256(date + normalizedDescription + amount)`, normalization = uppercase +
  collapse whitespace.
- OFX: `fitid`, bank-provided, unique per account statement.

When the import feature lands, a duplicate detected via the unique-sparse index's
`DuplicateKeyException` should be treated as "skip, already imported" — not surfaced as a 409 to
the user. Leave a short comment-free stub or simply don't build the import command handler yet;
just make sure `TransactionDocument` has both fields and §9's (now §10's) indexes exist so the
schema is future-proof.

## 8. New ground — category auto-suggestion

`GET /transactions/suggest-category?description=...` → `CategorySuggestionService`
(`domain/service/`, no ports, no Spring annotations — pure function over the description string
and the keyword table). Per `docs/product-definition.md` §Module 2, this is **keyword-based, no
ML** — a `Map<String, CategoryKeywords>`-style lookup: normalize the input description
(uppercase, strip accents) and match against a static keyword-to-category table defined in that
doc. Returns `Mono<String>` (nullable `categoryId`) — `null`/empty when no keyword matches, per the
contract's `nullable: true` on the response's `categoryId`. This is a read-only convenience
endpoint; it does not persist anything and needs no command handler.

## 9. Budget alerts — no hook needed

The original spec had a §9 here defining a no-op `BudgetAlertTrigger` hook point for
`CreateTransactionCommandHandler`/`UpdateTransactionCommandHandler` to call, ahead of the
not-yet-built budgets feature. Per
`docs/adr/ADR-005-transaction-immutability-and-deferred-recurring-generation.md` (Decision 3),
this is dropped entirely: `docs/adr/ADR-004-defer-projection-fanout-until-budgets.md` already
decided that this category of speculative build-out is exactly the "infrastructure ahead of the
feature" it rejected. When `specs/008-budgets` is built, its `BudgetSpentProjector` subscribes to
the existing Change Stream/SQS pipeline directly — 004 needs zero code, not even a seam, for this.

## 10. Indexes — programmatic, not annotation-based

Same as 003: indexes are created at startup in `infrastructure/config/MongoIndexConfig.java`
via `ApplicationRunner` + `mongoTemplate.indexOps(...).createIndex(...)`, chained onto the
existing `.then(...)` sequence. Add every index from `data-model.md`'s table:

```java
Index transactionOwnerIndex = new Index().on("ownerId", Sort.Direction.ASC);
Index transactionOwnerDateIndex = new Index()
    .on("ownerId", Sort.Direction.ASC).on("date", Sort.Direction.DESC);
Index transactionAccountFeedIndex = new Index()
    .on("ownerId", Sort.Direction.ASC).on("accountId", Sort.Direction.ASC).on("date", Sort.Direction.DESC);
Index transactionInvoiceIndex = new Index()
    .on("ownerId", Sort.Direction.ASC).on("invoiceId", Sort.Direction.ASC);
Index transactionCategoryIndex = new Index()
    .on("ownerId", Sort.Direction.ASC).on("categoryId", Sort.Direction.ASC).on("date", Sort.Direction.DESC);
Index transactionImportHashIndex = new Index()
    .on("ownerId", Sort.Direction.ASC).on("importHash", Sort.Direction.ASC).unique().sparse();
Index transactionFitidIndex = new Index()
    .on("ownerId", Sort.Direction.ASC).on("fitid", Sort.Direction.ASC).unique().sparse();
Index recurringSeriesIndex = new Index().on("recurringSeriesId", Sort.Direction.ASC).sparse();
Index installmentSeriesIndex = new Index().on("installmentSeriesId", Sort.Direction.ASC).sparse();
Index transferPairIndex = new Index().on("transferPairId", Sort.Direction.ASC).sparse();
```

This closes the remaining half of the open checklist item at
`docs/architecture-contract.md` §8 ("Define the `ownerId` indexes per collection in each
feature's `data-model.md`") — Accounts closed its half in 003; Transactions closes the rest here.

Docs: [Spring Data MongoDB — Index creation](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/document-indexes.html).

## 11. Command/query handlers — layering example

The simple path (`CreateTransactionCommandHandler`, `mode = SINGLE`) is structurally identical to
`CreateAccountCommandHandler`: validate (§2's XOR check), build the `Transaction` domain object
with `.ownerId(ownerId)`, resolve category suggestion only if the client asked for it (it doesn't
auto-apply — `categoryId` is either client-supplied or left null), call
`transactionRepository.save(...)`, return — no post-write hook of any kind (§9). `CreateTransferCommand`
(§5), `CreateRecurringTransactionCommand`/`CreateInstallmentCommand` (§6) are the three
handlers with genuinely new shapes — everything else in this feature reuses patterns already
established by Accounts/Categories.

## 12. Error responses — mostly reused, two additions

Reuse the existing hierarchy (`NotFoundException` → 404, `ConflictException` → 409,
`BusinessException`/`DomainException` → 422) via the existing
`application/GlobalExceptionHandler.java` — no new `@ExceptionHandler` methods needed. Two new
*exception instances* (not new classes) are introduced by this feature:

- XOR-constraint violation (§2) → `BusinessException` (422).
- `PATCH` request containing any field outside the `description`/`categoryId`/`notes`/`tags`
  whitelist (§6, ADR-005) → `BusinessException` (422).

Transfer-atomicity failures (§5) don't need special handling — if the `TransactionalOperator`
rolls back, the underlying Mongo/driver exception propagates and is mapped like any other
persistence failure; no bespoke exception type required.

## 13. Testing

Mirror the existing structure, extended for this feature's new behaviors:

- **Unit** (`*Test`): one per command/query handler, Mockito-mocked ports, `StepVerifier`
  assertions — same shape as `CreateAccountCommandHandlerTest`. Include a `CategorySuggestionServiceTest`
  (pure function, table-driven over the keyword list).
- **Integration** (`*IT`): extend `AbstractIntegrationTest`. Add a `TransactionSteps` helper
  mirroring `AccountSteps` (wraps `WebTestClient`, sets the `accessToken` cookie via
  `UserSteps.createAndGetAccessToken()`).
- **Tenancy test** (mandatory, P2): cross-tenant isolation — user A cannot `GET`/`PATCH`
  user B's transaction (expect 404).
- **New, mandatory: transfer-atomicity test.** Force a partial failure on the second leg (e.g., a
  Mockito spy on the repository that throws after the first `save`, or a duplicate-key collision
  engineered via a pre-existing `transferPairId`) and assert **neither** leg is persisted —
  the whole point of §5's `TransactionalOperator` wrapping. Without this test, a regression that
  silently drops the `.as(transactionalOperator::transactional)` call would go unnoticed.
- **Installment generation tests**: assert the correct count of generated instances, correct
  `amount` split (including the remainder-centavo rule), and correct series-id linkage.
- **New, mandatory: deferred-recurring-generation tests.** Creating a series with a future start
  date generates zero `Transaction` instances at creation time (only the series document);
  `RecurringTransactionGenerationJob` generates exactly the due instance(s) and advances
  `nextOccurrenceDate`; nothing is generated past `endDate`.
- **New, mandatory: whitelist-rejection test.** A `PATCH` containing `amount` (or any
  non-whitelisted field) is rejected with 422 and the document is unchanged.

Docs: [Project Reactor — `StepVerifier`](https://projectreactor.io/docs/test/release/reference/index.html).

---

## Summary checklist

This checklist is superseded by `specs/004-transactions/tasks/` — each task file there carries its
own scope and acceptance criteria; treat that folder as the actual build order. Kept here only as
a one-line-per-unit index:

- [x] `Transaction` domain record + `TransactionDocument` (`@Version` included) — done
- [x] `TransactionMongoRepository` (thin) + `TransactionRepositoryAdapter` — done
- [x] `CreateTransactionCommandHandler` (SINGLE mode) with the account/invoice XOR check — done
- [ ] `tasks/001-shared-port-extensions.md` — `saveAll`, `existsByTransferPairId`, recurring-series ports
- [ ] `tasks/002-transfer.md` — `CreateTransferCommandHandler`
- [ ] `tasks/003-recurring-series-and-instance.md` — `RecurringTransactionSeries` + `CreateRecurringTransactionCommandHandler`
- [ ] `tasks/004-recurring-generation-job.md` — `RecurringTransactionGenerationJob`
- [ ] `tasks/005-installment.md` — `CreateInstallmentCommandHandler`
- [ ] `tasks/006-update-restricted.md` — `UpdateTransactionCommandHandler` (whitelist-only patch, no delete)
- [ ] `tasks/007-list-and-get-queries.md` — `ListTransactionsQueryHandler` + `GetTransactionQueryHandler`
- [ ] `tasks/008-category-suggestion.md` — `CategorySuggestionService` + endpoint
- [ ] `tasks/009-controller-and-indexes.md` — `TransactionController` wiring + `MongoIndexConfig`
