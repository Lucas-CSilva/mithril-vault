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

**Constraint:** Transactions are append-only. `PATCH` may change only `description`, `categoryId`,
`notes`, `tags`; every other field is immutable once created, and no transaction is ever deleted.
Enforced at the domain layer. See `docs/adr/ADR-005-transaction-immutability-and-deferred-recurring-generation.md`.

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

### `recurring_transaction_series`

The recurring definition — not an instance. `CreateRecurringTransactionCommandHandler` writes one
of these per series alongside the due-or-earlier `Transaction` instance(s);
`RecurringTransactionGenerationJob` reads it to generate each future instance as its date arrives.
See `docs/adr/ADR-005-transaction-immutability-and-deferred-recurring-generation.md`.

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. |
| `recurringSeriesId` | String (UUID) | Shared with every generated `Transaction.recurringSeriesId` |
| `frequency` | String | Enum: `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `BIMONTHLY`, `QUARTERLY`, `SEMIANNUAL`, `ANNUAL` |
| `endDate` | Date (LocalDate) | Nullable. No instance is generated once `nextOccurrenceDate > endDate`. |
| `nextOccurrenceDate` | Date (LocalDate) | The date of the next instance to generate. Advanced by `frequency` each time the job fires. |
| `type` | String | Template field — copied onto each generated `Transaction` |
| `amount` | Int64 | Template field |
| `description` | String | Template field |
| `categoryId` | String (UUID) | Template field, nullable |
| `paymentMethod` | String | Template field, nullable |
| `accountId` | String (UUID) | Template field |
| `tags` | Array\<String\> | Template field |
| `notes` | String | Template field, nullable |
| `createdAt` | Date | UTC instant |
| `_version` | Int64 | Optimistic locking |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `{ ownerId: 1, nextOccurrenceDate: 1 }` | Non-unique | — | `RecurringTransactionGenerationJob`'s due-series query |
| `recurringSeriesId` | Unique | — | One series document per series id |

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

### Deferred recurring generation rule
`CreateRecurringTransactionCommandHandler` inserts only the instance(s) due today or earlier, plus
one `recurring_transaction_series` document holding the series template and
`nextOccurrenceDate`. `RecurringTransactionGenerationJob` (scheduled, distributed-lock-guarded)
inserts each remaining instance as its date arrives and advances `nextOccurrenceDate`. No instance
is ever inserted before its own date. See ADR-005.

### Transaction immutability rule
A transaction, once created, is never deleted and only `description`, `categoryId`, `notes`, and
`tags` may be edited. All other fields (`amount`, `date`, `type`, `accountId`/`invoiceId`,
`paymentMethod`) are permanent. See ADR-005.

### Installment rule
N transactions, each with `amount = totalAmount / N` (integer division), with any remainder centavo added to installment 1. Each installment is assigned to the corresponding monthly invoice.

### Deduplication keys
- OFX: `fitid` (bank-provided, unique per account statement)
- CSV: `importHash = SHA-256(date + normalizedDescription + amount)` where normalization = uppercase + collapse whitespace

