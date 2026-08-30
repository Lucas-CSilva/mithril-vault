# ADR-005: Transactions are an immutable ledger; recurring instances are generated lazily, not eagerly

> **Document metadata**
> - **ID:** ADR-005
> - **Status:** Accepted
> - **Author:** Architect
> - **Date:** 2026-08-14
> - **Linked PRD:** PRD-004 — Transactions (FR-012, FR-013, and a new deferred-generation FR)
> - **Linked Spec:** `specs/004-transactions/data-model.md`,
>   `specs/004-transactions/implementation-notes.md`
> - **Related ADRs:** `docs/adr/ADR-002-invoice-generation-via-sqs.md` (reconciler-job precedent
>   this ADR's `RecurringTransactionGenerationJob` follows), `docs/adr/ADR-003-materialized-derived-balances.md`
>   (established the insert-only Change Stream trigger this ADR's both decisions are downstream
>   of), `docs/adr/ADR-004-defer-projection-fanout-until-budgets.md` (the budget-hook removal below
>   follows the same "don't build ahead of the feature" reasoning already accepted there)

---

## Context

`specs/004-transactions/implementation-notes.md` was written before ADR-002/003/004 existed. Two
of its assumptions no longer hold once the balance-projection pipeline they introduced is in
place:

**1. The projection pipeline only reacts to inserts.** `AccountBalanceChangeStreamListener`
watches the `transactions` collection filtered to `operationType: insert` (ADR-003). The original
004 spec's `UpdateTransactionCommandHandler` (in-place field edits) and
`DeleteTransactionCommandHandler` + `editScope`/`deleteScope` (delete-and-regenerate a recurring
series from an edited date forward) are both invisible to that filter. A balance-affecting edit
(amount, date, destination account) or a hard delete would never reach
`AccountBalanceProjector` — the materialized `accounts.currentBalance` would silently drift from
the actual transaction history with no path back to correctness, since nothing re-triggers a
projection for a document that was merely updated or removed in place.

**2. `AccountBalanceProjector` applies `$inc` unconditionally at insert time — there is no date
gate.** The original RECURRING design generated the entire series (today through the horizon: the
chosen `endDate`, or 12 months ahead) as real `Transaction` documents at creation time. Every one
of those is an insert, so every one of them immediately changes `accounts.currentBalance` — a
subscription due five months from now would reduce today's balance the moment the series is
created, not when the charge actually occurs.

Both problems trace back to the same root cause: the projection trigger is "a document was
inserted into `transactions`," and the original 004 design has code paths that mutate, remove, or
prematurely insert documents without that being the correct signal.

---

## Decision

### 1. Transactions are an append-only ledger; editing is a narrow whitelist; deletion doesn't exist

`PATCH /transactions/{id}` may change only `description`, `categoryId`, `notes`, and `tags`. Any
other field present in the request body (`amount`, `date`, `type`, `accountId`/`invoiceId`,
`paymentMethod`) is rejected with a 422 — the whole request is rejected, not partially applied.
There is no `DELETE /transactions/{id}` at all; a transaction, once created, exists forever.

Every field on the whitelist is display/organizational metadata that no projection reads —
`AccountBalanceProjector`, `InvoiceTotalProjector`, and any future `BudgetSpentProjector` only
ever consume `type`/`amount`/`accountId`/`invoiceId`, none of which are editable. This means a
whitelisted edit is correctly invisible to the insert-only Change Stream filter — there is nothing
for a projector to redo, so no listener or projector code changes are needed at all.

Edits apply to exactly one transaction id. There is no `editScope`, no bulk edit across a
recurring/installment series — the original "this and all future" concept is removed entirely,
not just narrowed.

This closes the correctness gap directly: a code path that could silently corrupt a materialized
balance is removed, rather than patched around.

### 2. RECURRING generation splits into an immediate due-instance insert plus a lazy scheduled job

`CreateRecurringTransactionCommandHandler` no longer generates the full horizon. It inserts only
the instance(s) whose `date` is today or earlier (in practice: the single instance dated `date`,
if `date <= today`), and persists one new **`RecurringTransactionSeries`** document — the
recurring definition, not an instance: `ownerId`, `recurringSeriesId`, `frequency`, `endDate`
(nullable), `nextOccurrenceDate`, and the transaction template fields needed to build each future
instance (`type`, `amount`, `description`, `categoryId`, `paymentMethod`, `accountId`, `tags`,
`notes`).

A new **`RecurringTransactionGenerationJob`** — `@Scheduled` daily, `@DistributedLock`-guarded,
same shape as `BalanceReconciliationJob`/`BalanceSnapshotJob` — streams every series where
`nextOccurrenceDate <= today` and (`endDate` is null or `>= nextOccurrenceDate`), inserts one
`Transaction` from the template for that occurrence, and advances `nextOccurrenceDate` by the
series' `frequency`. This is the same "reconciler, not a one-shot batch" property ADR-002
established for invoice generation: a missed job run doesn't lose an occurrence, it's just picked
up (possibly late) the next cycle, and the job re-derives what's due from the database on every
run rather than remembering what it already published.

INSTALLMENT is explicitly **not** changed by this decision. Installments target `invoiceId`, not
`accountId`, and a future invoice's `totalAmount` is supposed to reflect known future charges —
that's the entire point of a parcelamento purchase: the user's invoice three months from now
already shows the R$50 installment today. All N installments are still inserted at creation time,
unchanged from the original spec.

### 3. The budget-alert hook is removed from 004's scope entirely

The original spec's §9 ("budget-alert trigger, stub/hook point only") called a no-op
`BudgetAlertTrigger` port from `CreateTransactionCommandHandler`/`UpdateTransactionCommandHandler`
after every write, as a seam for the not-yet-built budgets feature. ADR-004 already decided this
category of speculative build-out directly: fan-out infrastructure (and, by the same reasoning,
hook points) for a consumer that doesn't exist yet is exactly the "infrastructure ahead of the
feature" ADR-004's Option A rejected. When `specs/008-budgets` is built, `BudgetSpentProjector`
subscribes to the same Change Stream/SQS pipeline on its own — 004 needs zero code, not even a
no-op seam, to support that.

---

## Status

`Accepted` — 2026-08-14

---

## Consequences

**Positive:**

- Removes a real correctness bug class (silent balance drift from in-place edits/deletes) instead
  of adding code to detect or repair it.
- No changes needed to `AccountBalanceChangeStreamListener`, `BalanceProjectionListener`, or any
  projector — the insert-only filter ADR-003 already built is exactly right once mutation/deletion
  of balance-relevant fields is disallowed by construction.
- RECURRING's materialized balance now only ever reflects transactions that have actually
  occurred, matching what "current balance" should mean for a real bank account.
- `RecurringTransactionGenerationJob` reuses an already-proven shape (`@DistributedLock`,
  reconciler semantics) rather than inventing new scheduling infrastructure.

**Negative:**

- Removes product capability the original spec had: no way to correct a transaction's amount,
  date, or destination after the fact, and no way to remove a mis-entered one. If that need
  surfaces later, the natural fix is reversal-style corrections (an offsetting entry + a new
  correct entry, both real inserts) — see Alternatives below — not reverting this decision.
- Adds a new collection (`recurring_transaction_series`) and a new scheduled job — one more moving
  part, though a small one that directly mirrors existing jobs.
- A recurring series' future instances don't exist as `Transaction` documents until their date
  arrives, so any read that needs to show "upcoming scheduled transactions" (a projected calendar
  view, say) cannot just query `transactions` — it would need to read `recurring_transaction_series`
  and compute upcoming occurrences separately. No such read exists yet in this codebase; flagged
  here so a future feature doesn't assume `transactions` already contains future-dated recurring
  instances.

**Risks and mitigations:**

- *Risk:* a future feature needs true corrections (fixing a wrong amount) and reaches for
  reintroducing in-place edits instead of the reversal pattern this ADR points to.
  *Mitigation:* the Alternatives section below names the reversal pattern explicitly as the
  intended fallback, so it doesn't need to be re-derived from scratch.
- *Risk:* `RecurringTransactionGenerationJob` is forgotten in the same way ADR-004 worried about
  its own trigger condition being forgotten. *Mitigation:* unlike ADR-004's deferred work, this
  job is part of this ADR's immediate Decision, not a future trigger condition — it ships with
  004, not later.

---

## Alternatives Considered

### Option A: Extend the Change Stream filter to also react to update/delete/replace (rejected)

Keep in-place edit and delete-and-regenerate as originally specced, and teach
`AccountBalanceChangeStreamListener` to handle `operationType: update`/`delete`/`replace` events,
computing a reversing `$inc` for the old value and a new `$inc` for the new value (or a full
reversal for a delete).

**Rejected because:** meaningfully more pipeline surface area (three more event shapes to reason
about, each with its own reversal-amount computation) to preserve an edit/delete capability that,
per direct product input, the app doesn't actually need — transactions should behave like a bank
statement line, not an editable spreadsheet cell. Diverges from `docs/architecture-contract.md`
P5 (immutability) rather than reinforcing it.

### Option B: Reversal-style corrections instead of a pure whitelist (deferred, not rejected)

Allow "editing" `amount`/`date`/`accountId` by writing an offsetting reversal transaction plus a
new corrected transaction, both real inserts — no listener changes needed, since both are
ordinary inserts the existing pipeline already handles correctly.

**Deferred, not rejected, because:** this is the correct answer *if* a correction capability is
ever needed, and is compatible with everything else in this decision — it would layer on top of,
not replace, the append-only invariant. It's deferred here only because no product requirement for
correcting amount/date/destination has surfaced (the user's direct instruction was that those
fields simply aren't editable), and building it now would be speculative in the same way ADR-004's
Option A was rejected. Named explicitly so it isn't re-litigated from scratch if the need arises.

### Option C: Eager RECURRING generation, but gate the projector on `date <= today` instead of gating what gets inserted (rejected)

Keep inserting the full future horizon at creation time, but have `AccountBalanceProjector` (or
`ApplyAccountBalanceProjectionCommandHandler`) skip applying `$inc` for transactions dated in the
future, applying it later via the reconciliation job or a similar date-driven sweep.

**Rejected because:** this still requires a scheduled component to eventually apply the deferred
`$inc`s — so it doesn't actually avoid adding a job, it just moves the "what's due today" logic
from the recurring feature into the shared balance-projection pipeline, coupling a
transaction-mode-specific concern (RECURRING's future-dating) into `AccountBalanceProjector`,
which every other transaction mode also flows through. Keeping the due-date gating inside
recurring generation (this ADR's Decision 2) keeps the projector simple and mode-agnostic, which is
the property ADR-003 was designed around.

### Option D: No series collection — re-derive `nextOccurrenceDate` from the last generated instance each job run (rejected)

Skip the new `recurring_transaction_series` collection; have
`RecurringTransactionGenerationJob` query `transactions` for the latest instance per
`recurringSeriesId` and compute the next occurrence from its `date` + `frequency` on every run.

**Rejected because:** requires a per-series scan (or an aggregation grouping by
`recurringSeriesId`, taking the max `date`) every cycle instead of an indexed
`nextOccurrenceDate <= today` point query, and loses `frequency`/`endDate` once they're needed but
no longer present on any single instance in a simple, queryable way. A dedicated series document is
the standard "job state" shape the codebase already uses (`ProjectionCheckpointDocument`,
`ProjectionLeaseDocument`) — reusing that shape here is consistent, not novel.

---

## References

- `docs/architecture-contract.md` P5 (immutability — reinforced, not superseded, by this ADR)
- `docs/adr/ADR-002-invoice-generation-via-sqs.md` — reconciler-job precedent
  `RecurringTransactionGenerationJob` follows directly
- `docs/adr/ADR-003-materialized-derived-balances.md` — the insert-only Change Stream trigger this
  ADR's both decisions are downstream of
- `docs/adr/ADR-004-defer-projection-fanout-until-budgets.md` — precedent for not building
  infrastructure ahead of the feature that would justify it, applied here to the removed
  budget-alert hook
- `specs/004-transactions/data-model.md` — `recurring_transaction_series` collection this ADR
  introduces
- `api/src/main/java/com/mithrilvault/api/infrastructure/scheduler/BalanceReconciliationJob.java`,
  `BalanceSnapshotJob.java` — the `@DistributedLock` job shape `RecurringTransactionGenerationJob`
  reuses
