# Data Model — Notifications (012)

In-app notification feed. Notifications are produced server-side by alert sources (budget
threshold checks on transaction save; the daily scheduled jobs for invoices, subscriptions,
and investment maturity) and consumed via the notification bell. No email or push (MVP).

## Collections

### `notifications`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. Set from JWT, never from request (P2). |
| `type` | String | Enum: `BUDGET_THRESHOLD_80`, `BUDGET_THRESHOLD_100`, `INVOICE_DUE_SOON`, `SUBSCRIPTION_CHARGE_UPCOMING`, `INVESTMENT_MATURITY_UPCOMING` |
| `message` | String | Server-rendered pt-BR display copy. Frozen at creation. |
| `read` | Boolean | False at creation. Set true via mark-read endpoints. |
| `payload` | Object | Embedded `NotificationPayload` (see below). Type-specific context for deep-linking. |
| `dedupeKey` | String | Natural idempotency key — prevents duplicate alerts (see below). |
| `createdAt` | Date | UTC instant the notification was generated. |
| `readAt` | Date | Nullable. UTC instant the caller marked it read. |
| `_version` | Int64 | Optimistic locking (`@Version`). |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | ⬜ Tenant-scoped listing (required by P2) |
| `{ ownerId: 1, read: 1 }` | Non-unique | — | ⬜ Unread-count query + read-state filter |
| `{ ownerId: 1, createdAt: -1 }` | Non-unique | — | ⬜ Feed listing, newest-first |
| `{ ownerId: 1, type: 1, createdAt: -1 }` | Non-unique | — | ⬜ Type-filtered feed listing |
| `{ ownerId: 1, dedupeKey: 1 }` | Unique | partial: `{ dedupeKey: { $exists: true } }` | ⬜ Idempotency — one notification per logical alert |
| `createdAt` | TTL | `expireAfterSeconds: 7776000` (90 days) | ⬜ Auto-expire stale notifications |

> The TTL index reaps notifications 90 days after `createdAt`. This bounds the collection
> without a manual purge job; the feed is a transient inbox, not a permanent ledger. Adjust
> the retention window during review if a longer history is desired.

---

### Embedded: `NotificationPayload`

Structured, type-specific context carried inside each notification document. Every field is
nullable; which fields are populated depends on `type`. Monetary fields are **Int64 centavos**,
never `Double` (P1).

| Field | BSON Type | Notes |
|---|---|---|
| `budgetId` | String (UUID) | Nullable. FK → budgets. Budget thresholds. |
| `categoryId` | String (UUID) | Nullable. FK → categories. Budget thresholds. |
| `budgetMonth` | String | Nullable. `YYYY-MM`. Budget period. Budget thresholds. |
| `limitAmount` | Int64 | Nullable. Centavos. Budget limit. Budget thresholds. |
| `spentAmount` | Int64 | Nullable. Centavos. Spend when threshold crossed. Budget thresholds. |
| `invoiceId` | String (UUID) | Nullable. FK → invoices. `INVOICE_DUE_SOON`. |
| `cardId` | String (UUID) | Nullable. FK → credit_cards. `INVOICE_DUE_SOON`. |
| `subscriptionId` | String (UUID) | Nullable. FK → subscriptions. `SUBSCRIPTION_CHARGE_UPCOMING`. |
| `investmentId` | String (UUID) | Nullable. FK → investments. `INVESTMENT_MATURITY_UPCOMING`. |
| `amount` | Int64 | Nullable. Centavos. Invoice total / charge amount / gross value at maturity. |
| `dueDate` | Date | Nullable. LocalDate. Invoice due date / nextChargeDate / maturityDate. |

`spentAmount` and `limitAmount` are snapshots captured at the moment the alert fired — they
are point-in-time context for the message, **not** the canonical derived budget figures
(those remain owned by the budget read side, P4). They are never recomputed from this document.

---

## Notification Type Enum

| Value | Source | Trigger | Payload fields populated |
|---|---|---|---|
| `BUDGET_THRESHOLD_80` | `BudgetAlertService` (from `TransactionCommandHandler`) | A budget crosses 80% of its limit on transaction save | `budgetId`, `categoryId`, `budgetMonth`, `limitAmount`, `spentAmount` |
| `BUDGET_THRESHOLD_100` | `BudgetAlertService` | A budget crosses 100% of its limit on transaction save | `budgetId`, `categoryId`, `budgetMonth`, `limitAmount`, `spentAmount` |
| `INVOICE_DUE_SOON` | Daily scheduled job (09:00 BRT) | Invoice due in ≤ 3 days | `invoiceId`, `cardId`, `amount`, `dueDate` |
| `SUBSCRIPTION_CHARGE_UPCOMING` | Daily scheduled job (09:00 BRT) | Subscription `nextChargeDate` ≤ 3 days | `subscriptionId`, `amount`, `dueDate` |
| `INVESTMENT_MATURITY_UPCOMING` | Daily scheduled job (09:00 BRT) | Investment `maturityDate` ≤ 7 days | `investmentId`, `amount`, `dueDate` |

---

## Idempotency — `dedupeKey`

Alert sources run repeatedly (budget checks on every transaction save; the maturity / charge /
invoice jobs every day), so the same logical alert can be evaluated many times. `dedupeKey`
plus the unique `{ ownerId, dedupeKey }` index guarantees one notification per logical alert
(P7 — money-adjacent commands are idempotent).

Key composition by type:

| Type | `dedupeKey` |
|---|---|
| `BUDGET_THRESHOLD_80` | `budget:{budgetId}:{budgetMonth}:80` |
| `BUDGET_THRESHOLD_100` | `budget:{budgetId}:{budgetMonth}:100` |
| `INVOICE_DUE_SOON` | `invoice:{invoiceId}:due` |
| `SUBSCRIPTION_CHARGE_UPCOMING` | `subscription:{subscriptionId}:{nextChargeDate}` |
| `INVESTMENT_MATURITY_UPCOMING` | `investment:{investmentId}:{maturityDate}` |

Creation uses an idempotent upsert keyed on `{ ownerId, dedupeKey }`: a duplicate insert is a
no-op, not an error and not a second notification. The budget keys include the threshold and
month so each threshold fires at most once per budget per month; the date-bearing job keys
re-arm when the underlying date changes (e.g. a subscription's next charge advances).

---

## Relationships

```
users (1) ──< notifications (many)        [notifications.ownerId]

notifications (0..1) ──> budgets          [payload.budgetId      — budget thresholds]
notifications (0..1) ──> categories       [payload.categoryId    — budget thresholds]
notifications (0..1) ──> invoices         [payload.invoiceId     — invoice due]
notifications (0..1) ──> credit_cards     [payload.cardId        — invoice due]
notifications (0..1) ──> subscriptions    [payload.subscriptionId — subscription charge]
notifications (0..1) ──> investments      [payload.investmentId  — investment maturity]
```

Payload FKs are soft references for deep-linking only — there is no cascade. If a referenced
aggregate is deleted, the notification remains valid as a historical record; the client tolerates
a dangling deep-link (and such notifications expire via the TTL index regardless).

---

## Lifecycle

```
(alert source fires)
   → idempotent upsert on { ownerId, dedupeKey }
   → notification created  read = false, readAt = null
   → PATCH /notifications/{id}/read   →  read = true,  readAt = now
   → PATCH /notifications/read-all    →  all unread → read = true, readAt = now
   → createdAt + 90 days              →  removed by TTL index
```

`read` only ever transitions false → true; there is no "mark unread". Marking an already-read
notification is a no-op (idempotent) and returns it unchanged.

---

