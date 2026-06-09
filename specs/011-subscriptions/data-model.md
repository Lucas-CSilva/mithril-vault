# Data Model — Subscriptions (011)

## Collections

### `subscriptions`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. |
| `name` | String | e.g. "Netflix", "Spotify", "Adobe Creative Cloud" |
| `billingCycle` | String | Enum: `MONTHLY`, `BIMONTHLY`, `QUARTERLY`, `SEMIANNUAL`, `ANNUAL` |
| `nextChargeDate` | Date | LocalDate. Must be kept up to date after each charge. |
| `categoryId` | String (UUID) | FK → categories |
| `paymentMethod` | String | Enum: `PIX`, `TED`, `DOC`, `DEBIT_CARD`, `CREDIT_CARD`, `BOLETO`, `CASH` |
| `creditCardId` | String (UUID) | Nullable. FK → credit_cards. When paid by credit card. |
| `color` | String | Nullable. Hex color code (e.g. `#7B2FBE`). User-assigned display color for visual identification in the UI. |
| `status` | String | Enum: `ACTIVE`, `PAUSED`, `CANCELLED` |
| `cancelledAt` | Date | Nullable. LocalDate. |
| `notes` | String | Nullable. |
| `linkedTransactionPattern` | String | Nullable. Keyword for auto-matching imports to this subscription. |
| `priceHistory` | Array | Embedded. Append-only list of `PriceEntry` (see below). At least one entry always exists. |
| `createdAt` | Date | UTC instant |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |
| `{ ownerId: 1, status: 1, nextChargeDate: 1 }` | Non-unique | — | Obligation radar + charge alert job |

---

### Embedded: `PriceEntry`

Each element in `subscriptions.priceHistory`:

| Field | BSON Type | Notes |
|---|---|---|
| `amount` | Int64 | Centavos. The price for this period. |
| `validFrom` | Date | LocalDate. First day this price was in effect. |

`priceHistory` is append-only. The current price is always the `amount` of the most recent entry. Entries are never modified or deleted.

---

## Derived Values (never stored)

### `currentAmount`
```
currentAmount = priceHistory[-1].amount   // most recent entry
```

### `monthlyEquivalentCost`

| Cycle | Multiplier | Computation |
|---|---|---|
| `MONTHLY` | × 1 | `currentAmount` |
| `BIMONTHLY` | × 0.5 | `(currentAmount × 5000) / 10000` |
| `QUARTERLY` | × 0.3333 | `(currentAmount × 3333) / 10000` |
| `SEMIANNUAL` | × 0.1667 | `(currentAmount × 1667) / 10000` |
| `ANNUAL` | × 0.0833 | `(currentAmount × 833) / 10000` |

All multiplications use integer arithmetic (multiply before divide per P1).

---

## Price History Semantics

When the user updates a subscription:
- If `amount` changed: append a new `PriceEntry` with `validFrom = today`
- If `amount` did not change: update metadata only (no new entry)

Same-day edits: if the most recent `PriceEntry.validFrom = today`, update it in place (no duplicate entries for the same day).

---

## Auto-Detection Logic

After any import confirmation, the system scans the newly created transactions for recurring patterns:
1. Group `transactions WHERE ownerId = X AND importSource IN [CSV, OFX]` by normalized description
2. Within each group, find subgroups where `amount` is identical
3. For each subgroup with ≥ 2 transactions at approximately monthly intervals (28–34 days), surface as a subscription suggestion
4. Filter out transactions already matched to an existing subscription via `linkedTransactionPattern`
5. Return suggestions — not automatically created. User confirms or dismisses each one.

---

## Charge Alert Job

`@Scheduled` daily at 09:00 BRT:
- Query: `{ status: ACTIVE, nextChargeDate: { $lte: today+3 } }` (all users)
- Emit `SubscriptionChargeAlertEvent` for each match, keyed on `(ownerId, subscriptionId, nextChargeDate)` to avoid duplicate alerts
