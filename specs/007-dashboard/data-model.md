# Data Model — Dashboard (007)

## No new collections

The dashboard is a pure read-side aggregation over existing collections. No data is stored specifically for the dashboard — all values are computed on demand.

**Collections read:**
- `accounts` (all active, for Saldo Líquido and KPI)
- `transactions` (current month and historical, for KPIs and charts)
- `invoices` (OPEN status, for Saldo Líquido and Obligation Radar)
- `budgets` + `transactions` (for Budget Progress, Phase 3.2)
- `subscriptions` (nextChargeDate ≤ 7 days, for Obligation Radar)

---

## Aggregation Descriptions

### Saldo Líquido Disponível

```
saldoLiquido = SUM(account.currentBalance, for all ACTIVE accounts owned by user)
             - SUM(invoice.totalAmount, for all OPEN invoices owned by user)
```

Per `docs/adr/ADR-003-materialized-derived-balances.md`, `account.currentBalance` and
`invoice.totalAmount` are materialized fields (003-accounts, 005-cards), not aggregated at read
time. Saldo Líquido is therefore a **second-order projection**: it composes two already-computed
projections rather than re-deriving either from `transactions`. The read becomes a cheap in-memory
sum over two small `$match` + `$sum(field)` queries (summing a stored field, not aggregating
transaction history) — no `$lookup` into `transactions` needed for this KPI.

### KPI Cards (current calendar month)

| KPI | Formula |
|---|---|
| Receitas | `SUM(amount WHERE type=CREDIT AND transferPairId IS NULL AND month = current)` |
| Despesas | `SUM(amount WHERE type=DEBIT AND transferPairId IS NULL AND invoiceId IS NULL AND month = current)` — excludes invoice payments and transfer debits |
| Investido no mês | `SUM(amount WHERE type=DEBIT AND categoryId = system Investimentos AND month = current)` |
| Saldo do mês | Receitas − Despesas |

### Cash Flow Chart

Period: 7d / 30d / 3m / 12m (user-selectable). Y-axis = total liquid balance at end of each day.

Algorithm: compute `currentBalance` for each day as a running balance starting from the first day of the period.

Index hint: `{ ownerId: 1, accountId: 1, date: -1 }` on transactions.

### Expense Distribution

Current month DEBIT transactions (excl. transfers) grouped by top-level category:
```
{ categoryId, totalAmount, percentage }
```
A category's `totalAmount` includes transactions from all its subcategories (resolved via a `$lookup` on the two-level hierarchy).

### Radar de Obrigações (next 7 days)

Two sources merged and sorted by due date ascending:
1. Invoices: `{ ownerId, status IN [OPEN, CLOSED], dueDate <= today+7 }` — shows as "Fatura [card name]"
2. Subscriptions: `{ ownerId, status = ACTIVE, nextChargeDate <= today+7 }` — shows as subscription name

### Budget Progress

All budgets for the current month with `spentAmount` projected (see specs/008-budgets for the aggregation). If Phase 3.2 is not yet built, this section returns an empty array.

---

## Performance Notes

All dashboard aggregations are scoped to `ownerId` (P2). For a single user's data the volumes are small enough that a single combined `/dashboard` endpoint with all aggregations is acceptable at MVP. If latency becomes a concern, these can be split into parallel calls on the frontend.

Suggested MongoDB aggregation approach: use `$facet` to run multiple sub-aggregations in a single query pass over the transactions collection.
