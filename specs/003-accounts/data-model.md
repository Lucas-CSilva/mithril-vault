# Data Model — Accounts (003)

## Collections

### `accounts`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. Set from JWT, never from request. |
| `name` | String | e.g. "Nubank", "Bradesco Corrente" |
| `type` | String | Enum: `CHECKING`, `SAVINGS`, `CASH`, `DIGITAL` |
| `institution` | String | Bank name, optional |
| `initialBalance` | Int64 | Centavos. Balance on the day the account was added. Adjusted by reconciliation. |
| `color` | String | Hex color, e.g. `#88C0D0` |
| `isActive` | Boolean | Soft-delete flag. Inactive accounts are hidden in the UI but retained. |
| `createdAt` | Date | UTC instant |
| `_version` | Int64 | Optimistic locking (`@Version`). Concurrent edits use this field. |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Tenant-scoped listing (required by P2) |
| `{ ownerId: 1, name: 1 }` | Unique | collation `{ locale: "pt", strength: 2 }` | Prevent duplicate account names per user |

---

## Derived Values (never stored)

### `currentBalance`

```
currentBalance = initialBalance
              + SUM(amount WHERE type = CREDIT AND accountId = this._id)
              - SUM(amount WHERE type = DEBIT  AND accountId = this._id)
```

Computed by a MongoDB aggregation pipeline (`$lookup` on `transactions`, `$group` with `$sum`).
Returned in `AccountResponse` only — never stored in the `accounts` document.

### `availableOnCard` (credit cards — different collection, see 005-cards)

Not part of this collection.

---

## Relationships

```
users (1) ──< accounts (many)
accounts (1) ──< transactions (many)      [transactions.accountId]
accounts (1) ──< invoices (many)          [invoices.paidFromAccountId — nullable]
accounts (1) ──< credit_cards (many)      [credit_cards.associatedAccountId — nullable]
accounts (1) ──< goals (many)             [goals.linkedAccountId — nullable]
```

---

## Reconciliation

When a user manually reconciles an account:

1. **Adjusting transaction path:** create a `CREDIT` or `DEBIT` transaction with `description = "Reconciliação"` and a flag `isReconciliation = true`. This transaction is shown distinctly in the feed.
2. **Direct adjustment path:** update `initialBalance` so that `currentBalance` matches the real balance. This is a direct document update (no transaction created).

Both paths result in the same derived `currentBalance`. The user chooses which path via the UI.
