# Task 005 — INSTALLMENT mode

## Scope

**In scope:** implement `CreateInstallmentCommandHandler` (currently a stub) — N transactions,
integer-division amount split with the remainder centavo on installment 1, each assigned to the
invoice for its own month. Unchanged from the original spec — ADR-005 explicitly does not affect
installments (see its Decision 2).

**Out of scope:** the actual credit-card/invoice resolution logic if it doesn't exist yet —
`TransactionOriginResolver` currently throws `NotImplementedException` for
`CREDIT_CARD`/`DEBIT_CARD` payment methods pending spec 005 (cards). If 005 isn't built yet when
this task starts, this handler is blocked on it and should stay a stub with a clear
`NotImplementedException` message referencing 005, same as today — do not build a parallel,
temporary invoice-resolution path just to unblock this task.

## Depends on

`tasks/001-shared-port-extensions.md` (`saveAll`). Effectively also depends on `005-cards` being
implemented (`TransactionOriginResolver`'s card/invoice resolution) — flag this dependency
explicitly when picking up this task; it may not be unblockable yet.

## Files touched

- `domain/commandhandler/transaction/CreateInstallmentCommandHandler.java` — replace the stub
- `domain/commandhandler/transaction/CreateInstallmentCommandHandlerTest.java` — new

## Acceptance Criteria

- **AC-T05-1:** An installment purchase of R$100,01 split into 3 installments generates amounts
  33,35 / 33,33 / 33,33 (centavos: 3335/3333/3333), summing exactly to 10001.
- **AC-T05-2:** Installment *k* is assigned to the invoice *k* months after the first
  installment's invoice, following the same closing-day resolution logic §2 of
  `implementation-notes.md` describes for `creditCardId`.
- **AC-T05-3:** All N installments share one `installmentSeriesId`, with `installmentNumber`/
  `totalInstallments` set correctly on each.
- **AC-T05-4:** A `paymentMethod` other than `CREDIT_CARD` (or a bare `accountId` destination
  instead of a card) is rejected — installments are credit-card-only, per the contract.

## Notes

No `TransactionalOperator` wrapping needed — a partial write here is an incomplete, regenerable
series, not a correctness bug (unlike transfers), same reasoning the original spec used.
