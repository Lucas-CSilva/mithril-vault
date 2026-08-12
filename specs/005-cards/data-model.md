# Data Model — Cards & Invoices (005)

## Collections

### `credit_cards`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. |
| `name` | String | e.g. "Nubank Ultravioleta" |
| `institution` | String | Issuing bank |
| `last4Digits` | String | Display only. Full PAN never stored. |
| `cardType` | String | Enum: `PHYSICAL`, `VIRTUAL` |
| `creditLimit` | Int64 | Centavos |
| `closingDay` | Int32 | Day of month fatura closes (1–28) |
| `dueDay` | Int32 | Day of month payment is due (1–28) |
| `associatedAccountId` | String (UUID) | Nullable. FK → accounts. Account that pays this card. |
| `color` | String | Hex |
| `isActive` | Boolean | Soft-delete |
| `createdAt` | Date | UTC instant |
| `_version` | Int64 | Optimistic locking |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |

---

### `invoices`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Denormalized for tenant-scoped queries. |
| `creditCardId` | String (UUID) | FK → credit_cards |
| `referenceMonth` | String | Format: `YYYY-MM` (e.g. "2026-06"). Treated as YearMonth. |
| `closingDate` | Date | Computed: `referenceMonth + closingDay` |
| `dueDate` | Date | Computed: typically `closingDate + (dueDay - closingDay)` days, may cross into next month |
| `status` | String | Enum: `OPEN`, `CLOSED`, `PAID` |
| `totalAmount` | Int64 | Centavos. Materialized projection, kept in sync by `InvoiceTotalProjector`. See [Derived Values](#derived-values-materialized-projections). |
| `paidAt` | Date | Nullable. UTC instant. |
| `paidFromAccountId` | String (UUID) | Nullable. FK → accounts. |
| `_version` | Int64 | Optimistic locking; also guards the reconciliation job's self-heal writes to `totalAmount` |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |
| `{ ownerId: 1, creditCardId: 1, referenceMonth: 1 }` | Unique | — | One invoice per card per month |
| `{ ownerId: 1, status: 1, dueDate: 1 }` | Non-unique | — | Dashboard obligation radar (open/closed, due soon) |

---

## Relationships

```
users (1) ──< credit_cards (many)
credit_cards (1) ──< invoices (many)           [invoices.creditCardId]
invoices (1) ──< transactions (many)           [transactions.invoiceId]
accounts (0..1) ──< credit_cards (many)        [credit_cards.associatedAccountId]
accounts (0..1) ──< invoices (many)            [invoices.paidFromAccountId]
```

---

## Derived Values (materialized projections)

> Per `docs/adr/ADR-003-materialized-derived-balances.md` (supersedes P4 of
> `docs/architecture-contract.md`), `invoice.totalAmount` is a **stored, materialized field**,
> updated incrementally rather than aggregated at read time. Unlike `accounts.currentBalance`
> (003-accounts), this feature has no existing data yet, so it's built with the pattern from day
> one — no backfill/migration needed. Component breakdown and sequence diagrams (shared with
> 003-accounts' `AccountBalanceProjector`) live in
> `docs/technical-solutions/materialized-projections.md`.

### `invoice.totalAmount`

**Definition** (the invariant the materialized field must always converge to):
```
totalAmount = SUM(transactions.amount WHERE invoiceId = this._id)
```

**How it's kept in sync:** `InvoiceTotalProjector` subscribes to the same MongoDB Change Stream
on `transactions` used by `AccountBalanceProjector` (003-accounts) and applies each
credit-card-transaction insert/reversal as an atomic `$inc` on `invoices.totalAmount`, scoped by
`transaction.invoiceId`. `InvoiceReadRepository.findOpenInvoice` and any future
"list invoices" read simply return the stored field — no aggregation on the request path. A
ground-truth aggregation pipeline (`$match` + `$group` + `$sum` over `transactions`, scoped by
`invoice_total_snapshots`, see below) is kept for the reconciliation job and admin tooling only.
Full pattern (checkpointing, idempotency guard, snapshot cadence, reconciliation/self-heal) in
ADR-003.

### `invoice_total_snapshots` (new collection)

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users |
| `invoiceId` | String (UUID) | FK → invoices |
| `asOfDate` | Date | Snapshot checkpoint date |
| `totalAmount` | Int64 | Centavos, as of `asOfDate` |
| `lastTransactionId` | String (UUID) | Last transaction included in this snapshot's sum |

An OPEN invoice accumulates transactions for at most one billing cycle, so this snapshot mostly
matters for reconciliation/audit tooling rather than bounding an otherwise-unbounded scan (unlike
account balances, an invoice's transaction set doesn't grow indefinitely).

### `creditCard.utilizedAmount`
```
utilizedAmount = totalAmount of the current OPEN invoice for this card
```
Reads the already-materialized `invoice.totalAmount` — no aggregation.

### `creditCard.availableLimit`
```
availableLimit = creditLimit - utilizedAmount
```

### `bestPurchaseDay`
```
bestPurchaseDay = closingDay + 1  (mod 28 if closingDay = 28)
```
A purchase on this day goes into the next invoice, maximizing the interest-free period.

---

## Invoice Lifecycle

> Full lifecycle state diagram and the invoice-generation sequence flows (card creation +
> monthly rollover) now live in `specs/005-cards/technical-solution.md` §3–4 — this section
> keeps only the field/index-relevant facts; see the tech-solution doc for the narrative and
> Mermaid diagrams. Generation mechanism (SQS-based, event-driven) is recorded in
> `docs/adr/ADR-002-invoice-generation-via-sqs.md`.

**Invoice auto-generation:** When a CreditCard is created, invoices for the current month and the next month are automatically generated. A scheduled job runs on the 1st of each month to generate invoices for the new month for all active cards. Both triggers publish a `GenerateInvoiceMessage` (see below) rather than creating the invoice inline — see ADR-002.

**Transaction assignment:** When a credit card transaction is created, it is assigned to the invoice whose `referenceMonth` corresponds to the transaction's billing month. A transaction dated before `closingDay` goes into that month's invoice; on or after `closingDay`, it goes into the next month's invoice.

**Payment atomicity (P7):** Paying an invoice is a single reactive transaction:
1. `invoice.status` → `PAID`, `paidAt` = now, `paidFromAccountId` = selected account
2. A DEBIT transaction is created on the selected account for `totalAmount` (or user-specified amount if partial payment)

---

## Queue Message — `GenerateInvoiceMessage`

Not a persisted collection — a message on the `mithril-vault-invoice-generation` SQS queue
(LocalStack locally). Published by card creation (current + next month) and by the monthly
rollover job (next month, per active card missing one). Consumed idempotently: a duplicate
delivery hits the unique index below and is treated as a no-op.

| Field | Type | Notes |
|---|---|---|
| `ownerId` | String (UUID) | Tenant scope, carried explicitly since there's no request-scoped security context in a queue consumer |
| `creditCardId` | String (UUID) | FK → `credit_cards` |
| `referenceMonth` | String | `YYYY-MM`, same format as `invoices.referenceMonth` |

Idempotency guarantee: the existing unique index `{ ownerId: 1, creditCardId: 1, referenceMonth: 1 }` on `invoices` (above) — no separate dedup table needed for this message.

**ShedLock:** the monthly rollover trigger is guarded by a distributed lock (`shedLock` collection, ShedLock's own Mongo schema — infra-only, no `ownerId`) so exactly one API instance fires the fan-out per cycle. See `technical-solution.md` §3.2.
