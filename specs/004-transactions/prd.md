# PRD: Transactions

> **Document metadata**
> - **ID:** PRD-004
> - **Status:** Approved (retroactive — written after `data-model.md` / `contracts/transaction.openapi.yaml`
>   were already in place, to give this feature a BA-level companion document)
> - **Author:** BA
> - **Last updated:** 2026-07-25
> - **Reviewers:** Lucas

---

## 1. Problem Statement

Every other feature in Mithril Vault (accounts, categories, cards) exists only to describe *where*
money lives or *how it's labeled*. None of them record money actually moving. Without a
Transactions feature, the app has no ledger — balances can't be computed, invoices can't
accumulate charges, and none of the product's core promise (a truthful picture of personal
finances) is deliverable.

Real money movement in Brazil isn't always a single, simple event: a purchase can repeat monthly
(subscriptions), split across future credit card bills (parcelamento), or be a transfer between
the user's own accounts rather than real spending. A transaction feature that only supported
"one row per event" would force the user to hand-enter every future installment or recurring
charge, which is exactly the kind of manual bookkeeping this app exists to remove.

---

## 2. Goals

- [x] Let the user record any money movement — one-off, recurring, installment-based, or a
      transfer between their own accounts — without hand-entering repeat or split entries.
- [x] Keep every transaction attributable to exactly one place it affects: a bank account balance
      or a credit card invoice — never both, never neither.
- [x] Preserve historical accuracy: a transaction's displayed source name/type never silently
      changes because the user renamed or deleted the underlying account/card later.
- [x] Make editing a recurring/installment series safe — changing "today forward" must never
      rewrite the past.
- [x] Lay the groundwork (fields + indexes only) for import-based entry (CSV/OFX) and budget
      alerts, without building those features yet.

### Non-Goals

- Not in scope: actually importing CSV/OFX files (feature 006).
- Not in scope: budget threshold alerts firing (feature 008) — only the write-side hook exists.
- Not in scope: ML/NLP-based category suggestion — keyword matching only.
- Not in scope: multi-currency — BRL centavos only, per the root money contract.

---

## 3. Users and Context

| Segment | Description | Volume / Impact |
|---|---|---|
| Single user (owner) | The one person whose finances this instance tracks (app is scoped multi-user for learning purposes, but each owner only ever sees their own data) | High — every screen depends on this feature |
| System (background) | Future recurring/installment generation, future budget-alert checks | Internal only |

---

## 4. Use Cases

### UC-001: Log a one-off transaction (SINGLE)

**Actor:** Owner
**Goal:** Record a single purchase or income event against an account or credit card invoice
**Precondition:** The target account (or credit card) exists and belongs to the owner

1. User opens "Add transaction" and enters type (DEBIT/CREDIT), amount, date, description.
2. User picks a destination: a bank account, or a credit card (system resolves it to the
   currently-open invoice based on the card's closing day vs. the transaction date).
3. User optionally sets category, payment method, tags, notes.
4. System saves one transaction, stamped with the account/card's current display name
   (`sourceName`/`sourceType`) as a permanent historical snapshot.

**Alternate path 2a — category left blank:** System offers a keyword-based suggested category;
user may accept, override, or ignore it. No category is auto-applied without user action.

### UC-002: Set up a recurring transaction (RECURRING)

**Actor:** Owner
**Goal:** Log a charge that repeats on a schedule (e.g., a monthly subscription) once, not every month
**Precondition:** Same as UC-001

1. User fills the same fields as UC-001, then chooses "Recurring" and picks a frequency
   (weekly, biweekly, monthly, bimonthly, quarterly, semiannual, annual) and, optionally, an end date.
2. System generates one transaction per occurrence, from the start date forward, either until the
   chosen end date or 12 months ahead if none was given.
3. All generated instances are linked by a shared series id so they can later be viewed, edited,
   or deleted together.

**Alternate path — editing a future instance:** User edits one instance and chooses "this and all
future" — system deletes and regenerates every instance from that date forward with the new
values. Past instances are never touched.

### UC-003: Split a credit card purchase into installments (INSTALLMENT)

**Actor:** Owner
**Goal:** Record a "parcelado" purchase so each installment lands on the correct month's invoice
**Precondition:** Destination is a credit card (installments are credit-card-only)

1. User enters the purchase as in UC-001, selects a credit card as the destination, and chooses
   "Installments," entering the total count (2–48).
2. System divides the total amount by the count (integer centavos division), adding any leftover
   centavo to the first installment so nothing is lost to rounding.
3. System creates one transaction per installment, each assigned to the invoice for its own month
   (installment *k* → the invoice *k* months after the first), all linked by a shared series id.

### UC-004: Transfer money between the owner's own accounts (TRANSFER)

**Actor:** Owner
**Goal:** Move money from one of their own accounts to another without it being counted as income or spending
**Precondition:** Both accounts exist and belong to the owner

1. User picks a source account, a destination account, an amount, and a date.
2. System creates two linked transactions — a DEBIT on the source account and a CREDIT on the
   destination account — sharing a transfer pair id, written together so that either both exist or
   neither does.
3. Re-submitting the same transfer pair id is a no-op (prevents duplicate transfers from a retried
   request).
4. Transfers are excluded from any income/expense totals computed elsewhere in the app.

---

## 5. Functional Requirements

| ID | Priority | Description |
|---|---|---|
| FR-001 | P0 | The system shall allow the owner to create a single transaction with type, amount, date, description, and exactly one destination (account or invoice). |
| FR-002 | P0 | The system shall reject a transaction that specifies both an account and an invoice, or neither. |
| FR-003 | P0 | The system shall resolve a credit-card destination to its correct open invoice based on the card's closing day and the transaction date. |
| FR-004 | P0 | The system shall snapshot the destination's display name and type onto the transaction at creation time, and never update that snapshot if the account/card is later renamed. |
| FR-005 | P0 | The system shall allow the owner to create a recurring transaction with a frequency and optional end date, generating instances from the start date forward (12 months ahead if no end date is given). |
| FR-006 | P0 | The system shall link all instances of a recurring series with a shared series identifier. |
| FR-007 | P0 | The system shall allow the owner to split a credit-card transaction into 2–48 installments, dividing the amount by integer centavo division and adding any remainder to the first installment. |
| FR-008 | P0 | The system shall assign each installment to the invoice corresponding to its position in the series (installment *k* → *k* months after the first installment's invoice). |
| FR-009 | P0 | The system shall allow the owner to transfer between two of their own accounts, creating a linked DEBIT and CREDIT pair written atomically. |
| FR-010 | P0 | The system shall treat a repeated transfer-pair identifier as a no-op rather than creating a duplicate transfer. |
| FR-011 | P0 | The system shall exclude transfers from income/expense totals wherever those totals are computed. |
| FR-012 | P1 | The system shall allow the owner to edit a transaction's `description`, `categoryId`, `notes`, and `tags`; no other field is editable and no transaction is deletable, on any transaction regardless of mode or age. |
| FR-013 | P1 | The system shall generate a recurring transaction's due-or-earlier instance(s) at creation time and generate each remaining future instance automatically as its date arrives, never before. |
| FR-014 | P1 | The system shall suggest a category based on keyword matching against the transaction description, without auto-applying it. |
| FR-015 | P2 | The system shall allow the owner to attach free-form tags and notes to a transaction. |
| FR-016 | P2 | The system shall provide filtered, paginated retrieval of transactions by account, invoice, category, type, payment method, date range, and description search. |

> Import-triggered creation (CSV/OFX) and budget-alert side effects are represented in the data
> model (dedup keys, hook point) but are out of scope for this PRD's functional requirements —
> see feature 006 (Import) and feature 008 (Budgets).

---

## 6. Non-Functional Requirements

| ID | Category | Description |
|---|---|---|
| NFR-001 | Correctness | All monetary values shall be stored and computed as integer centavos; no floating-point representation shall ever be used for money. |
| NFR-002 | Data integrity | A transfer's two legs shall be written within a single database transaction — a partial write (one leg only) shall never be observable. |
| NFR-003 | Tenancy | Every transaction read and write shall be scoped to the authenticated owner; no query shall return or accept another owner's data. |
| NFR-004 | Auditability | A not-owned transaction shall return 404, not 403, to avoid confirming existence to a non-owner. |
| NFR-005 | Concurrency | Transactions shall support optimistic-locking versioning so concurrent edits (e.g., a series regeneration overlapping a manual edit) are detected rather than silently overwritten. |

---

## 7. Acceptance Criteria

| ID | Linked Requirements | Criterion |
|---|---|---|
| AC-001 | FR-001, FR-002 | Given a create request with both `accountId` and `invoiceId` set (or both null), when submitted, then the system rejects it with a validation error and creates nothing. |
| AC-002 | FR-004 | Given a transaction created against an account named "Nubank", when that account is later renamed to "Nubank Antigo", then the transaction's stored source name still reads "Nubank". |
| AC-003 | FR-007 | Given an installment purchase of R$100,01 split into 3 installments, when the system generates the series, then the amounts are R$33,35 / R$33,33 / R$33,33 (remainder centavo on the first) and sum exactly to R$100,01. |
| AC-004 | FR-009, FR-010 | Given a transfer request that fails after the first leg is written, when the write is retried, then either both legs exist or neither does — never one. Given the same transfer-pair id submitted twice, then only one transfer pair exists. |
| AC-005 | FR-012 | Given a `PATCH` request that includes any field other than `description`, `categoryId`, `notes`, or `tags` (e.g. `amount`), when submitted, then the system rejects it with a 422 and persists no change. |
| AC-007 | FR-013 | Given a recurring series created today with a monthly frequency and no end date, when the create request completes, then exactly the due-or-earlier instance exists and no future-dated instance exists until its own date arrives via the scheduled generation job. |
| AC-006 | NFR-003, NFR-004 | Given owner A attempts to GET, PATCH, or DELETE a transaction owned by owner B, then the system returns 404. |

---

## 8. Out of Scope

- CSV/OFX file import and duplicate-detection UX (feature 006-import).
- Budget threshold evaluation and notification (feature 008-budgets).
- Multi-currency or non-BRL amounts.
- Machine-learning category classification (keyword matching only).
- Editing the `mode` of an existing transaction (e.g., converting a SINGLE into a RECURRING series after creation).
- Editing a transaction's `amount`, `date`, `type`, `accountId`/`invoiceId`, or `paymentMethod` after creation — transactions are an append-only ledger (see `docs/adr/ADR-005-transaction-immutability-and-deferred-recurring-generation.md`); only `description`, `categoryId`, `notes`, and `tags` are editable.
- Deleting a transaction, under any circumstance.

---

## Appendix: Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Is `paymentMethod` required for every mode, or inferred server-side for TRANSFER (always `TRANSFER`) and INSTALLMENT (credit-card implies `CREDIT_CARD`)? | Architect | Open |
| 2 | Should the owner be able to cap total recurring instances below 12 months, or is the end date the only lever? | BA | Resolved — end date is the only lever; no separate max-count field. |
