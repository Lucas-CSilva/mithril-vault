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
| `paidAt` | Date | Nullable. UTC instant. |
| `paidFromAccountId` | String (UUID) | Nullable. FK → accounts. |
| `_version` | Int64 | Optimistic locking |

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

## Derived Values (never stored)

### `invoice.totalAmount`
```
totalAmount = SUM(transactions.amount WHERE invoiceId = this._id)
```

### `creditCard.utilizedAmount`
```
utilizedAmount = totalAmount of the current OPEN invoice for this card
```

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

```
OPEN ──────────────────────────────────────────────► CLOSED
  │  (auto: on closingDate, or manually triggered)      │
  │                                                      ▼
  │                                                    PAID
  │                                        (manual: user registers payment)
  │
  └── transactions are added while OPEN
```

**Invoice auto-generation:** When a CreditCard is created, invoices for the current month and the next month are automatically generated. A scheduled job runs on the 1st of each month to generate invoices for the new month for all active cards.

**Transaction assignment:** When a credit card transaction is created, it is assigned to the invoice whose `referenceMonth` corresponds to the transaction's billing month. A transaction dated before `closingDay` goes into that month's invoice; on or after `closingDay`, it goes into the next month's invoice.

**Payment atomicity (P7):** Paying an invoice is a single reactive transaction:
1. `invoice.status` → `PAID`, `paidAt` = now, `paidFromAccountId` = selected account
2. A DEBIT transaction is created on the selected account for `totalAmount` (or user-specified amount if partial payment)
