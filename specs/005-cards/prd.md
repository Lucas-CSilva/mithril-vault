# PRD: Cards & Invoices

> **Document metadata**
> - **ID:** PRD-005
> - **Status:** Approved (retroactive — written after `data-model.md` /
>   `contracts/card.openapi.yaml` were already in place, to give this feature a BA-level
>   companion document, same as `004-transactions`)
> - **Author:** BA
> - **Last updated:** 2026-07-26
> - **Reviewers:** Lucas

---

## 1. Problem Statement

Credit cards are where a large share of Brazilian personal spending actually happens, but a
credit card purchase doesn't affect a bank balance the moment it's made — it accumulates on a
monthly "fatura" (invoice) that closes on a fixed day, becomes payable, and is eventually settled
from a bank account. Without a Cards & Invoices feature, 004-transactions has nowhere correct to
put a credit-card charge: `TransactionOriginResolver` already resolves a credit-card destination
to "the currently open invoice," but that invoice has to exist first, and today it never does —
nothing creates it.

The product also promises the user two numbers they check constantly: how much of a card's limit
is still available, and how much a given invoice will cost when it closes. Both numbers only mean
something if they reflect every transaction posted so far, in real time — a stale or manually
recalculated number defeats the purpose of automating this over a spreadsheet.

---

## 2. Goals

- [x] Let the user register a credit card (limit, closing day, due day, paying account) and have
      its invoices exist automatically — the user should never have to manually create a "June
      fatura."
- [x] Always have exactly one OPEN invoice per card that new credit-card transactions land in,
      resolved automatically from the transaction date and the card's closing day.
- [x] Show the user always-current `availableLimit` and invoice `totalAmount` without any stale
      cached number and without the user ever triggering a recalculation.
- [x] Let the user close an invoice and register its payment as a single, atomic action that
      updates both the invoice and the paying account.
- [x] Ensure invoice creation keeps working correctly regardless of how many cards/users exist —
      this is a deliberate scale exercise (see NFR-006/NFR-007), not just a feature requirement.

### Non-Goals

- Not in scope: partial-payment reconciliation logic beyond "mark PAID with whatever amount the
  user enters" (data-model.md already scopes this: "user manages this manually").
- Not in scope: credit card interest/late-fee calculation.
- Not in scope: importing a card statement (feature 006-import) — this PRD only covers cards
  created and paid manually.
- Not in scope: choosing/implementing the exact queue technology — that is an architecture
  decision, recorded separately in `docs/adr/ADR-002-invoice-generation-via-sqs.md`.

---

## 3. Users and Context

| Segment | Description | Volume / Impact |
|---|---|---|
| Single user (owner) | The one person whose cards/invoices this instance tracks (app is scoped multi-user for learning purposes, but each owner only ever sees their own data) | High — every card/invoice screen depends on this feature |
| System (background) | Monthly invoice rollover, future budget/dashboard aggregations reading `totalAmount`/`availableLimit` | Internal only |

---

## 4. Use Cases

### UC-001: Register a credit card

**Actor:** Owner
**Goal:** Add a credit card so future credit-card transactions have somewhere to land
**Precondition:** None (first card) or the paying account, if set, belongs to the owner

1. User enters name, institution, last 4 digits, card type, credit limit, closing day, due day,
   and optionally the account that pays this card and a display color.
2. System saves the card and ensures an OPEN invoice exists for the current reference month and
   the next one, without the user taking any further action.
3. User immediately sees the card in their wallet with `availableLimit == creditLimit` (no
   transactions posted yet).

### UC-002: Post a credit-card transaction into the correct invoice

**Actor:** Owner (indirectly, via 004-transactions)
**Goal:** Have a credit-card purchase land on the invoice that will actually bill it
**Precondition:** The card exists and has an OPEN invoice covering the transaction's date

1. User creates a transaction against a credit card (004-transactions UC-001/UC-003).
2. System resolves the transaction's date against the card's `closingDay`: before closing day →
   current reference month's invoice; on/after closing day → next month's invoice.
3. Transaction is saved with that `invoiceId`; the invoice's `totalAmount` reflects it on the very
   next read (derived, never cached).

**Error path 2a — no OPEN invoice exists for that date:** Today this 404s
(`TransactionOriginResolver.findOpenInvoice`); this PRD's FR-002/FR-003 close that gap so the
invoice always already exists by the time a transaction needs it.

### UC-003: Check available limit and invoice total

**Actor:** Owner
**Goal:** See a trustworthy, current utilization/available-limit number
**Precondition:** Card exists

1. User opens the card wallet or an invoice detail screen.
2. System computes `totalAmount` (sum of the invoice's transactions) and `availableLimit`
   (`creditLimit - totalAmount` of the current OPEN invoice) at read time, via aggregation.
3. Numbers are always consistent with the transactions currently on file — no manual refresh, no
   possibility of drift between a stored number and the transactions that back it.

### UC-004: Close and pay an invoice

**Actor:** Owner
**Goal:** Settle a fatura from the paying account
**Precondition:** Invoice is OPEN (to close) or CLOSED (to pay)

1. User closes an invoice (manually, or the system closes it automatically on `closingDate`).
2. User registers payment, selecting an account and (optionally) a partial amount.
3. System atomically marks the invoice PAID and creates a DEBIT transaction on the selected
   account for the paid amount.

### UC-005: Monthly invoice rollover at scale

**Actor:** System (no human actor)
**Goal:** Every active card has an OPEN invoice for the upcoming reference month, without a human
ever running this by hand, and without assuming the card count stays small
**Precondition:** None — runs unconditionally on schedule

1. On the 1st of each month, the system determines which active cards do not yet have an invoice
   for the new reference month.
2. System creates exactly one invoice per such card, without creating a duplicate if the card
   already has one (idempotent — safe to re-run, safe if the previous run was interrupted).
3. This must remain correct and non-blocking whether there are 10 cards or 10 million — see
   NFR-006/NFR-007. The mechanism (synchronous loop vs. a decoupled/queued fan-out) is an
   architecture decision (ADR-002), not a product requirement, but the *observable behavior*
   above (every active card ends up with next month's invoice, exactly once, without a human
   intervening) is.

---

## 5. Functional Requirements

| ID | Priority | Description |
|---|---|---|
| FR-001 | P0 | The system shall allow the owner to register a credit card with name, institution, last 4 digits, card type, credit limit, closing day, due day, and optionally a paying account and color. |
| FR-002 | P0 | The system shall automatically create invoices for the current and next reference month when a credit card is created, without requiring a separate user action. |
| FR-003 | P0 | The system shall automatically create the next reference month's invoice for every active card, once per month, without requiring a human to trigger it. |
| FR-004 | P0 | The system shall never create two invoices for the same card and reference month (unique per card+month). |
| FR-005 | P0 | The system shall resolve a credit-card transaction to the invoice whose reference month matches the transaction's date against the card's closing day (before closing day → current month; on/after → next month). |
| FR-006 | P0 | The system shall compute `invoice.totalAmount`, `creditCard.utilizedAmount`, and `creditCard.availableLimit` at read time from the current set of transactions; none of these values shall be persisted. |
| FR-007 | P0 | The system shall allow the owner to close an OPEN invoice (`OPEN → CLOSED`). |
| FR-008 | P0 | The system shall allow the owner to pay a CLOSED invoice, atomically transitioning it to PAID and creating a DEBIT transaction on the selected account for the paid amount. |
| FR-009 | P1 | The system shall allow the owner to update a card's mutable fields (name, limit, closing/due day, paying account, color) and deactivate (soft-delete) a card. |
| FR-010 | P1 | The system shall list a card's invoices (most recent first, bounded by a `months` parameter) and provide invoice detail including its transactions. |

---

## 6. Non-Functional Requirements

| ID | Category | Description |
|---|---|---|
| NFR-001 | Correctness | All monetary values (`creditLimit`, `totalAmount`, `availableLimit`) shall be integer centavos; no floating-point representation shall ever be used. |
| NFR-002 | Data integrity | Invoice payment (status → PAID + DEBIT transaction) shall be written within a single database transaction — a partial write shall never be observable. |
| NFR-003 | Tenancy | Every card and invoice read/write shall be scoped to the authenticated owner. |
| NFR-004 | Auditability | A not-owned card or invoice shall return 404, not 403. |
| NFR-005 | Concurrency | Cards and invoices shall support optimistic-locking versioning. |
| NFR-006 | Scalability | Monthly invoice rollover shall not depend on holding the full active-card set in memory at once, and shall not block on a single card's failure preventing others from being processed — this NFR is deliberately over-specified relative to current single-user scale, as a scale-handling learning exercise (see ADR-002). |
| NFR-007 | Resilience | A single lost or failed invoice-generation attempt for one card shall be self-healing within one month's rollover cycle, without manual intervention or data loss. |
| NFR-008 | Idempotency | Re-attempting invoice generation for a card/month that already has an invoice shall be a no-op, never a duplicate or an error surfaced to the user. |

---

## 7. Acceptance Criteria

| ID | Linked Requirements | Criterion |
|---|---|---|
| AC-001 | FR-001, FR-002 | Given a new credit card submitted with valid fields, when created, then invoices exist for both the current and next reference month immediately after the response returns. |
| AC-002 | FR-004, FR-008 | Given a card that already has an invoice for reference month "2026-07", when invoice generation runs again for that card/month, then no second invoice is created. |
| AC-003 | FR-005 | Given a card with `closingDay = 10`, when a transaction dated the 9th is posted, then it lands on the current month's invoice; when a transaction dated the 10th is posted, then it lands on next month's invoice. |
| AC-004 | FR-006 | Given an invoice with three transactions of R$50, R$30, R$20, when `totalAmount` is read, then it returns R$100 without any prior write having stored that number. |
| AC-005 | FR-008, NFR-002 | Given a CLOSED invoice, when payment is registered and the DEBIT-transaction write fails, then the invoice's status is not observed as PAID (both changes commit or neither does). |
| AC-006 | FR-003, NFR-007 | Given a card that was somehow missed by one monthly rollover run (simulated failure), when the next month's rollover runs, then that card has an invoice created (self-heals within one cycle). |
| AC-007 | NFR-003, NFR-004 | Given owner A attempts to GET/PATCH/DELETE a card or invoice owned by owner B, then the system returns 404. |

---

## 8. Out of Scope

- Card statement import (feature 006-import).
- Interest, late fees, or minimum-payment calculations.
- Automated dispute/chargeback handling.
- The specific queue/broker technology and distributed-locking mechanism for the monthly job —
  an architecture decision, see `docs/adr/ADR-002-invoice-generation-via-sqs.md` and
  `specs/005-cards/technical-solution.md`.

---

## Appendix: Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Should invoice auto-close on `closingDate` be a scheduled job of its own, or left manual (FR-007 only covers manual close)? | Architect | Open — `technical-solution.md` should note this as a natural extension of the same scheduler introduced for rollover. |
| 2 | Is a card's `associatedAccountId` required, or can a card exist with no linked paying account until the first payment? | BA | Resolved — optional, per `data-model.md` (`associatedAccountId` nullable); `payInvoice` takes an explicit `accountId` per payment. |
