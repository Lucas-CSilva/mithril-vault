# Task 003 — RecurringTransactionSeries model + due-instance creation

## Scope

**In scope:** the `RecurringTransactionSeries` domain model (see
`data-model.md`'s `recurring_transaction_series` table for the field list), and rewriting
`CreateRecurringTransactionCommandHandler` (currently a stub) to insert only the due-or-earlier
instance(s) plus the series document — per
`docs/adr/ADR-005-transaction-immutability-and-deferred-recurring-generation.md`.

**Out of scope:** generating any instance whose date is in the future — that's
`tasks/004-recurring-generation-job.md`. This task's handler never inserts more than the
instance(s) already due at creation time (in practice: zero or one).

## Depends on

`tasks/001-shared-port-extensions.md` (`RecurringSeriesRepository`, and coordinate the
`RecurringTransactionSeries` shape between the two tasks if built in parallel).

## Files touched

- `domain/model/RecurringTransactionSeries.java` — new record, fields per `data-model.md`
- `domain/commandhandler/transaction/CreateRecurringTransactionCommandHandler.java` — replace the
  stub
- `domain/commandhandler/transaction/CreateRecurringTransactionCommandHandlerTest.java` — new

## Acceptance Criteria

- **AC-T03-1:** A recurring command with `date` in the future (after today) creates exactly one
  `RecurringTransactionSeries` document with `nextOccurrenceDate == command.date()`, and **zero**
  `Transaction` documents.
- **AC-T03-2:** A recurring command with `date` today or in the past creates exactly one
  `Transaction` instance dated `command.date()` (with `isRecurring = true` and the series'
  `recurringSeriesId`), plus a `RecurringTransactionSeries` document with `nextOccurrenceDate`
  advanced one step past `command.date()` by `recurring.frequency()`.
- **AC-T03-3:** The series document carries every template field
  (`type`/`amount`/`description`/`categoryId`/`paymentMethod`/`accountId`/`tags`/`notes`) needed
  for `RecurringTransactionGenerationJob` (004) to build a correct future instance without
  re-reading the original command.
- **AC-T03-4:** `recurring.endDate()` set before `command.date()` → 422 validation error (an
  end date before the start date can never generate anything).

## Notes

`RECURRING` only ever targets `accountId` (per ADR-005's Decision 2 reasoning — the whole point of
deferral is that `accountId`-targeted balance shouldn't move early; card-based recurring charges
aren't in this feature's scope regardless, per `TransactionOriginResolver`'s current
`NotImplementedException` for card payment methods). Don't build any invoice-targeting path for
RECURRING here.
