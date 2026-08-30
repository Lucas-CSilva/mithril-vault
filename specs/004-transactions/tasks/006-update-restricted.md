# Task 006 — UpdateTransactionCommandHandler (whitelist-only, no delete)

## Scope

**In scope:** `UpdateTransactionCommandHandler` — a narrow `PATCH` that may change only
`description`, `categoryId`, `notes`, `tags`. Any other field present in the request is rejected
(422), and nothing is persisted. This task does **not** include a delete handler — there is no
`DeleteTransactionCommandHandler` in this feature; per
`docs/adr/ADR-005-transaction-immutability-and-deferred-recurring-generation.md`, a transaction is
never deletable.

**Out of scope:** `editScope`, bulk/series edits, any change to
`AccountBalanceChangeStreamListener` — the whitelisted fields never feed a projection, so a
whitelist-only update never needs to be observed by the change stream (and the existing
`operationType: insert` filter already ignores plain updates correctly).

## Depends on

Nothing new — reuses `TransactionRepository`/`TransactionReadRepository` as they already exist.

## Files touched

- `domain/command/transaction/UpdateTransactionCommand.java` — new, fields limited to
  `description`, `categoryId`, `notes`, `tags` (all optional/nullable — a `PATCH` only sets what's
  present)
- `domain/commandhandler/transaction/UpdateTransactionCommandHandler.java` — new
- `domain/commandhandler/transaction/UpdateTransactionCommandHandlerTest.java` — new

## Acceptance Criteria

- **AC-T06-1:** A `PATCH` with only `description`/`categoryId`/`notes`/`tags` set succeeds,
  updates exactly those fields, and leaves every other field (including `amount`, `date`,
  `accountId`) untouched.
- **AC-T06-2:** A request body containing any field outside that whitelist (e.g. `amount`) is
  rejected with 422 `BusinessException`, and the document is unchanged — verify by re-reading the
  document after the rejected request.
- **AC-T06-3:** Patching a transaction owned by a different owner → 404 `NotFoundException` (P2 —
  not-owned resource returns 404, not 403).
- **AC-T06-4:** A stale `@Version` on the request results in the existing optimistic-lock conflict
  handling (409), consistent with how `Account`/`Invoice` already handle concurrent edits.
- **AC-T06-5:** There is no `DELETE /transactions/{id}` route reachable at all (verify at the
  controller/contract level in `tasks/009-controller-and-indexes.md`, not duplicated here).

## Notes

Because `CreateTransactionCommand` uses no `*Request` DTO layer (per `api/CLAUDE.md`'s "no request
DTO" rule), `UpdateTransactionCommand` should follow the same convention — bind the PATCH body
directly to this command record, with Jakarta validation annotations on it, not a separate
`UpdateTransactionRequest`. The 422-on-unknown-field behavior needs an explicit check in the
handler (or `@JsonIgnoreProperties(ignoreUnknown = false)` combined with strict deserialization) —
Jackson silently drops unknown JSON properties by default, which would NOT reject an `amount` field
sent alongside a valid patch; make sure whichever binding path is chosen actually surfaces that as
a 422, not a silent no-op ignore.
