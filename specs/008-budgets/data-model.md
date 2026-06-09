# Data Model — Budgets (008)

## Collections

### `budgets`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. |
| `categoryId` | String (UUID) | FK → categories |
| `month` | String | Format: `YYYY-MM` (e.g. "2026-06"). Treated as YearMonth. |
| `limitAmount` | Int64 | Centavos |
| `isRecurring` | Boolean | If true, auto-generate for future months on the 1st of each month |
| `alertAt80` | Boolean | Trigger notification when spentAmount crosses 80% of limitAmount |
| `alertAt100` | Boolean | Trigger notification when spentAmount crosses 100% of limitAmount |
| `createdAt` | Date | UTC instant |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |
| `{ ownerId: 1, month: 1 }` | Non-unique | — | List budgets for a given month |
| `{ ownerId: 1, categoryId: 1, month: 1 }` | Unique | — | One budget per category per month per user |
| `isRecurring` | Non-unique | partial `{ isRecurring: true }` | Monthly generation job filter |

---

## Derived Values (never stored)

### `spentAmount`

```
spentAmount = SUM(transactions.amount
              WHERE ownerId = budget.ownerId
                AND type = DEBIT
                AND transferPairId IS NULL
                AND date BETWEEN first_of_month(budget.month) AND last_of_month(budget.month)
                AND categoryId IN (budget.categoryId, ...subcategory ids))
```

**Budget aggregation rule (P2-level hierarchy):** Since the category hierarchy is at most two levels deep, subcategory resolution is a single query with no recursion:
1. Fetch `budget.categoryId`'s children: `{ parentId: budget.categoryId }`
2. Filter transactions where `categoryId IN (budget.categoryId, child1, child2, ...)`

This `$lookup` + `$group` pipeline is owned by `BudgetReadRepository`.

---

## Alert Logic

**Triggers:** Called from `TransactionCommandHandler` after every CREATE or UPDATE.
1. For the affected `(ownerId, categoryId, month)`, find all budgets that match (including parent-category budgets)
2. Recompute `spentAmount` for each matching budget
3. If the previous `spentAmount` was below a threshold and the new value crosses it, emit `BudgetAlertEvent`

Budget alert events are stored as `notifications` (see In-App Notifications section in implementation-plan.md).

---

## Recurring Budget Auto-Generation

A `@Scheduled` job runs at `00:01` on the 1st of each month:
- Query: `{ isRecurring: true }` (all users)
- For each, ensure a budget document exists for the new month with the same `limitAmount`, `alertAt80`, `alertAt100`, and `isRecurring = true`
- Insert is idempotent (unique index on `{ ownerId, categoryId, month }` prevents duplicates)
