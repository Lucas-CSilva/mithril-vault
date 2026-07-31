# Data Model — Transactions (004)

## Collections

### `transactions`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. Set from JWT. |
| `type` | String | Enum: `DEBIT`, `CREDIT` |
| `amount` | Int64 | Centavos. Always positive. |
| `date` | Date (LocalDate) | When it occurred — stored as `ISODate` at midnight UTC for date-only semantics |
| `description` | String | From bank statement or user-entered |
| `categoryId` | String (UUID) | FK → categories |
| `paymentMethod` | String | Enum: `PIX`, `TED`, `DOC`, `DEBIT_CARD`, `CREDIT_CARD`, `BOLETO`, `CASH`, `TRANSFER` |
| `accountId` | String (UUID) | Nullable. FK → accounts. Exactly one of `accountId` / `invoiceId` must be non-null. |
| `invoiceId` | String (UUID) | Nullable. FK → invoices. |
| `sourceName` | String | Display name of the originating account or card, embedded at write time. Denormalized at creation; not updated when the account/card is renamed. Preserves historical display state. |
| `sourceType` | String | Enum: `ACCOUNT`, `CARD`. Whether the transaction was entered via a bank account or a credit card invoice. |
| `tags` | Array\<String\> | User-defined labels |
| `notes` | String | Free text, nullable |
| `isRecurring` | Boolean | `true` if part of a recurring series |
| `recurringSeriesId` | String (UUID) | Nullable. Groups all instances of a recurring series |
| `installmentSeriesId` | String (UUID) | Nullable. Groups installments of a parcelamento |
| `installmentNumber` | Int32 | Nullable. e.g. 2 (of 6) |
| `totalInstallments` | Int32 | Nullable. e.g. 6 |
| `transferPairId` | String (UUID) | Nullable. Links the two legs of a transfer (DEBIT + CREDIT) |
| `importHash` | String | Nullable. SHA-256(date + normalizedDescription + amount). Dedup key for CSV imports. |
| `fitid` | String | Nullable. OFX FITID. Dedup key for OFX imports. |
| `importSource` | String | Nullable. Enum: `MANUAL`, `CSV`, `OFX` |
| `isReconciliation` | Boolean | `true` if this is an adjusting transaction created by reconciliation |
| `appliedProjections` | Array\<String\> | Defaults to `[]` on insert. Idempotency guard for the materialized-balance pipeline — e.g. `["accountBalance"]` once `AccountBalanceProjector` has applied this transaction. See `docs/adr/ADR-003-materialized-derived-balances.md`. |
| `createdAt` | Date | UTC instant |
| `_version` | Int64 | Optimistic locking |

**Constraint:** Exactly one of `accountId` or `invoiceId` must be non-null. Enforced at the domain layer.

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |
| `{ ownerId: 1, date: -1 }` | Non-unique | — | Default sort for transaction feed |
| `{ ownerId: 1, accountId: 1, date: -1 }` | Non-unique | — | Per-account feed + balance aggregation |
| `{ ownerId: 1, invoiceId: 1 }` | Non-unique | — | Invoice transaction list |
| `{ ownerId: 1, categoryId: 1, date: -1 }` | Non-unique | — | Budget aggregations |
| `{ ownerId: 1, importHash: 1 }` | Unique | sparse | CSV deduplication |
| `{ ownerId: 1, fitid: 1 }` | Unique | sparse | OFX deduplication |
| `recurringSeriesId` | Non-unique | sparse | Edit/delete all-forward on recurring series |
| `installmentSeriesId` | Non-unique | sparse | Installment series lookup |
| `transferPairId` | Non-unique | sparse | Transfer pair lookup |

---

## Business Rules

### Account vs Invoice constraint
`accountId != null XOR invoiceId != null`. A transaction lives in exactly one place.

### Transfer rule
Creates two linked transactions atomically:
- DEBIT from source account (`transferPairId = X`, `paymentMethod = TRANSFER`)
- CREDIT to destination account (`transferPairId = X`, `paymentMethod = TRANSFER`)

Idempotency key: `transferPairId`. Re-submitting the same `transferPairId` is a no-op.
Transfers are excluded from income/expense KPI totals.

### Recurring series edit rule
When a recurring instance is edited, all instances from the edited date forward are deleted and regenerated with the updated parameters. Past instances are immutable.

### Installment rule
N transactions, each with `amount = totalAmount / N` (integer division), with any remainder centavo added to installment 1. Each installment is assigned to the corresponding monthly invoice.

### Deduplication keys
- OFX: `fitid` (bank-provided, unique per account statement)
- CSV: `importHash = SHA-256(date + normalizedDescription + amount)` where normalization = uppercase + collapse whitespace

### Budget alert trigger
After every transaction creation or update, the system must check if any active budget for `(ownerId, categoryId, month)` has crossed the 80% or 100% threshold. This is a post-write side-effect, not a domain invariant.
