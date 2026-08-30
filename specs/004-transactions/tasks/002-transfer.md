# Task 002 — TRANSFER mode

## Scope

**In scope:** implement `CreateTransferCommandHandler` (currently a stub throwing
`NotImplementedException`) for real: two linked legs (DEBIT from `command.accountId()`, CREDIT to
`command.transfer().destinationAccountId()`), written atomically, idempotent on `transferPairId`.

**Out of scope:** any change to `AccountBalanceChangeStreamListener`/`AccountBalanceProjector` —
both legs are ordinary inserts and the existing pipeline already applies the correct signed `$inc`
per leg based on `type` (DEBIT/CREDIT), nothing here needs to know about projections at all.

## Depends on

`tasks/001-shared-port-extensions.md` (`saveAll`, `existsByTransferPairId`).

## Files touched

- `domain/commandhandler/transaction/CreateTransferCommandHandler.java` — replace the stub
- `domain/model/Transaction.java` — add a `transferLeg(...)`-style factory (mirrors the existing
  `accountTransaction`/`debitCardTransaction`/`creditCardTransaction` factories), or reuse
  `accountTransaction` and set `transferPairId`/`paymentMethod = TRANSFER` after construction —
  whichever keeps `Transaction` consistent with its existing factory-method style
- `domain/commandhandler/transaction/CreateTransferCommandHandlerTest.java` — new
- `*IT` — extend `TransactionSteps`-based integration tests with a transfer scenario

## Acceptance Criteria

- **AC-T02-1:** A transfer with a valid source and destination account (both owned by the caller)
  creates exactly two transactions sharing one `transferPairId`, `paymentMethod = TRANSFER`, one
  `type = DEBIT` on `command.accountId()`, one `type = CREDIT` on
  `command.transfer().destinationAccountId()`.
- **AC-T02-2:** Forcing a failure after the first leg's write (Mockito spy throwing on the second
  `save`, or a duplicate-key collision engineered via a pre-existing `transferPairId` on one leg)
  results in **neither** leg persisted — verifies the `TransactionalOperator` wrapping actually
  works, not just that the code compiles with it present.
- **AC-T02-3:** Re-submitting a create request with the same `transfer.transferPairId()` that
  already exists for this owner is a no-op — no new documents are created, and the handler returns
  successfully (either the existing pair, or an empty/idempotent response — pick one and document
  it in the handler's Javadoc-free comment-free code via a clear method name, not a comment).
- **AC-T02-4:** `destinationAccountId` not owned by the caller (or nonexistent) → 404
  `NotFoundException`, no documents created.
- **AC-T02-5:** `transfer.transferPairId()` omitted → server generates one (`UUID.randomUUID()`)
  before the idempotency check.

## Notes

`TransactionalOperator` is already an auto-configured bean (see `implementation-notes.md` §5) —
no new Spring config needed. The idempotency check (`existsByTransferPairId`) must run *before*
the transactional `saveAll`, not inside it — if the pair already exists, short-circuit without
opening a transaction at all.
