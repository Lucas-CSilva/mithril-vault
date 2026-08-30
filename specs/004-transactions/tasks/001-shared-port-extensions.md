# Task 001 — Shared port extensions

## Scope

**In scope:** the port (interface) surface every later task depends on. No handler logic, no
business rules — just method signatures and their Mongo-adapter implementations.

- `TransactionRepository.saveAll(List<Transaction>)` — returns `Flux<Transaction>`. Needed by
  transfer (002), recurring instance creation (003), installment (005).
- `TransactionReadRepository.existsByTransferPairId(String ownerId, String transferPairId)` —
  returns `Mono<Boolean>`. Needed by transfer (002) for idempotency.
- New `RecurringSeriesRepository` (write port) — `save(RecurringTransactionSeries)`,
  `advance(String seriesId, LocalDate nextOccurrenceDate, Long expectedVersion)` (optimistic-lock
  update of just `nextOccurrenceDate` + `_version`).
- New `RecurringSeriesReadRepository` (read port) — `findDueSeries(LocalDate asOf)` returning
  `Flux<RecurringTransactionSeries>` for every series where `nextOccurrenceDate <= asOf` and
  (`endDate` is null or `endDate >= nextOccurrenceDate`).

**Out of scope:** any command handler, the scheduled job, the `RecurringTransactionSeries` domain
model itself (defined in 003, this task only needs to know its shape to write the repository
signatures against it — coordinate with 003 or land them together).

## Depends on

Nothing — this is the first task in the sequence.

## Files touched

- `domain/port/TransactionRepository.java` — add `saveAll`
- `domain/port/TransactionReadRepository.java` — add `existsByTransferPairId`
- `domain/port/RecurringSeriesRepository.java` — new
- `domain/port/RecurringSeriesReadRepository.java` — new
- `infrastructure/adapter/persistence/TransactionRepositoryAdapter.java` — implement `saveAll`
- `infrastructure/adapter/persistence/TransactionReadRepositoryAdapter.java` (or equivalent) —
  implement `existsByTransferPairId`
- `infrastructure/adapter/persistence/RecurringSeriesRepositoryAdapter.java` — new
- `infrastructure/persistence/document/RecurringTransactionSeriesDocument.java` — new (mirrors
  `TransactionDocument`'s `@Getter/@Setter/@SuperBuilder/@FieldNameConstants/@Document` shape)
- `infrastructure/mapper/RecurringTransactionSeriesMapper.java` — new (MapStruct, document ↔ domain)

## Acceptance Criteria

- **AC-T01-1:** `TransactionRepository.saveAll(List.of(a, b))` persists both documents and returns
  both with generated ids.
- **AC-T01-2:** `existsByTransferPairId` returns `true` only when a transaction with that
  `(ownerId, transferPairId)` pair already exists; `false` for an owner-scoped miss (existing pair
  under a different owner does not count).
- **AC-T01-3:** `findDueSeries(today)` returns a series with `nextOccurrenceDate == today`, does
  not return one with `nextOccurrenceDate` in the future, and does not return one whose `endDate`
  is before `nextOccurrenceDate`.
- **AC-T01-4:** `advance` updates `nextOccurrenceDate` and increments `_version`; a stale
  `expectedVersion` fails with the existing optimistic-lock conflict path (same as `Account`/
  `Invoice`'s `@Version` handling elsewhere in the codebase).

## Notes

`existsByTransferPairId` must be owner-scoped — a `transferPairId` collision across two different
owners is not a duplicate (P2 tenancy: no query on a user-owned collection may be unscoped).
`saveAll` on `TransactionRepository` is a thin passthrough to
`TransactionMongoRepository.saveAll(...)` mapped through `TransactionMapper`, same shape as the
existing single-document `save`.
