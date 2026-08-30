# Task 007 — ListTransactionsQueryHandler + GetTransactionQueryHandler

## Scope

**In scope:** the read side. `GetTransactionQueryHandler` (single lookup, owner-scoped) and
`ListTransactionsQueryHandler` (the richest filter set in the codebase so far, plus pagination) —
per `implementation-notes.md` §4's `Criteria` sketch. No architectural change from the original
spec; ADR-002/003/004/005 don't affect reads.

**Out of scope:** any new derived/materialized field — this task reads `TransactionDocument`
fields directly, no aggregation.

## Depends on

Nothing new.

## Files touched

- `domain/query/transaction/ListTransactionsQuery.java` — new
- `domain/queryhandler/transaction/ListTransactionsQueryHandler.java` — new
- `domain/queryhandler/transaction/GetTransactionQueryHandler.java` — new
- `application/response/TransactionPageResponse.java` — new (`content`, `page`, `size`,
  `totalElements`, `totalPages`, per the contract's `TransactionPage` schema)
- `infrastructure/adapter/persistence/TransactionRepositoryAdapter.java` — add the paginated
  `Criteria`-based list query (first paginated endpoint in this codebase — `Account.list` returns
  a plain `Flux`; this needs `Query.with(pageable)` + a separate `count()` for `totalElements`)

## Acceptance Criteria

- **AC-T07-1:** Each filter (`accountId`, `invoiceId`, `categoryId`, `type`, `paymentMethod`,
  `startDate`/`endDate`, `search`) narrows results correctly in isolation.
- **AC-T07-2:** Filters combine with AND semantics (e.g. `accountId` + `categoryId` together
  narrows further than either alone).
- **AC-T07-3:** `search` is a case-insensitive substring match on `description`
  (`Pattern.quote`-escaped, per §4's sketch — no regex injection from user input).
- **AC-T07-4:** Results sort by `date` descending by default; pagination shape
  (`page`/`size`/`totalElements`/`totalPages`) matches `TransactionPageResponse`.
- **AC-T07-5:** All queries are owner-scoped — a filter combination can never return another
  owner's transaction, and `GetTransactionQueryHandler` on a not-owned id → 404.

## Notes

Every filter is optional and additive — build the `Criteria` incrementally, always starting from
`ownerId.is(ownerId)` (P2: an unscoped query on a user-owned collection is a defect). Reuse
`PageRequest`/`Pageable` from Spring Data, same as `implementation-notes.md` §4 already sketches.
