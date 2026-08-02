# Mithril Vault — Implementation Plan

**Version:** 1.0
**Status:** Active
**Last Updated:** June 2026

> Build order follows `docs/product-definition.md` Part 5. Each phase depends on the
> previous being complete. Schema-first workflow applies everywhere: spec → review → tests →
> implementation.

---

## Workflow per feature

1. **Spec** — write/review `specs/[###-feature]/data-model.md` and `contracts/*.openapi.yaml`
2. **Backend domain** — models, ports, commands, queries, handlers (pure unit-testable)
3. **Backend infrastructure** — Mongo documents, repositories, aggregation pipelines
4. **Backend application** — controllers, response DTOs, mappers
5. **Backend tests** — unit (`*Test`), integration (`*IT` via Testcontainers), tenancy isolation
6. **Frontend types** — hand-write `types.ts` to mirror backend response Records exactly
7. **Frontend data layer** — `api.ts`, `keys.ts`, React Query hooks
8. **Frontend UI** — components (four states: loading, error, empty, loaded)
9. **E2E smoke** — golden path through the running stack

---

## Cross-Cutting Pre-Flight (before or during Phase 1)

These must be in place before feature implementation begins.

### Backend
- [ x] JWT infrastructure — `JwtProvider` (sign + verify), key config in `application.yaml`
- [ x] Observability wiring (correlation IDs via Micrometer, see ADR-001) — add `micrometer-tracing-bridge-otel` (or `-brave`) + `context-propagation`; enable `Hooks.enableAutomaticContextPropagation()`; a `WebFilter` reads/generates the `X-Correlation-Id` request header, sets it on the current `Observation` and the response header. **No** `Mono.deferContextual` correlation-id plumbing in handlers — the trace context surfaces to logs via MDC (`traceId`/`spanId` → `correlationId`)
- [ x] Structured JSON logging — Logback JSON encoder (`logstash-logback-encoder` or equivalent); log pattern includes the Micrometer-provided `traceId`/`correlationId` MDC keys
- [ x] ArchUnit test suite — enforce P3 dependency rules (domain must not import application/infrastructure; application must not import infrastructure)
- [ x] `AbstractIntegrationTest` ✅ (exists) — verify replica-set Testcontainers config is correct

### Frontend
- [ x] React Query `QueryClientProvider` wired in `app/layout.tsx`
- [ x] `shared/types/Centavos.ts` — branded type, `centavos()` constructor, `formatBRL()`
- [ x] `shared/utils/index.ts` — re-export `formatBRL`, date helpers, `cn`
- [ x] `core/ports/ApiClient.ts` — typed HTTP interface (`get`, `post`, `put`, `patch`, `delete`)
- [ x] `core/ports/AuthGateway.ts` — `login`, `register`, `refresh`, `logout` interface
- [ x] `core/services/HttpApiClient.ts` — `fetch` wrapper; `credentials: 'include'`; centralized 401 → refresh → retry → redirect
- [ x] `core/contexts/ApiClientProvider.tsx` — injects `HttpApiClient` implementation
- [ x] ESLint `eslint-plugin-boundaries` config — enforce feature/core/shared import rules
- [ x] `middleware.ts` — redirect unauthenticated users from `(app)` routes to `/login`
- [ x] App shell — authenticated layout: navigation sidebar/header, notification bell placeholder

---

## Phase 1 — Foundation

**Spec files:** `specs/001-auth`, `specs/002-categories`, `specs/003-accounts`

### Feature 1.1 — Authentication

#### Backend
- [ ] `User` domain model (Record: `id`, `email`, `passwordHash`, `displayName`, `status`, `createdAt`)
- [ ] `UserStatus` enum (`ACTIVE`, `DISABLED`)
- [ ] `UserRepository` write port (save, findByEmail)
- [ ] `UserReadRepository` read port (findById for profile)
- [ ] `RegisterUserCommand` (email, rawPassword, displayName) + `RegisterUserCommandHandler` (email uniqueness check, BCrypt hash, persist)
- [ ] `LoginCommand` (email, rawPassword) + `LoginCommandHandler` (credential validation, issue access JWT + rotating refresh token)
- [ ] `RefreshCommand` (refresh token from cookie) + `RefreshCommandHandler` (validate, rotate, issue new pair)
- [ ] Refresh token rotation — every `POST /refresh` issues a new refresh token and invalidates the presented one (set `revokedAt`, link the new token via `replacedByTokenHash`)
- [ ] Refresh token reuse detection — if an already-revoked token is presented (theft indicator), revoke ALL refresh tokens for that `userId` and return `401`, forcing re-login
- [ ] `LogoutCommand` + `LogoutCommandHandler` (invalidate refresh token)
- [ ] `JwtProvider` in infrastructure (sign with RS256 or HS256; configurable secret/key)
- [ ] `RefreshTokenDocument` — stored in Mongo with `tokenHash`, `userId`, `issuedAt`, `expiresAt`, `revokedAt` (nullable), `replacedByTokenHash` (nullable — links the rotation chain for reuse detection)
- [ ] `UserDocument` + `UserMongoRepository`
- [ ] `AuthController` — `POST /register`, `POST /login`, `POST /refresh`, `POST /logout`
- [ ] Spring Security config — `SecurityConfig` updated: permit auth endpoints + Swagger paths; JWT resource server filter chain
- [ ] `RegisterUserCommandHandlerTest` (unit) + `AuthControllerIT` (integration)
- [ ] `RefreshCommandHandlerTest` — covers rotation and reuse detection (replaying a revoked token revokes all user tokens and yields `401`)
- [ ] `UserMongoRepositoryIT` (Testcontainers)

#### Frontend
- [ ] `features/auth/types.ts` — `LoginRequest`, `RegisterRequest`, `AuthResponse` (accessToken omitted — cookie-only; displayName, email)
- [ ] `features/auth/schema.ts` — zod schemas for login + register forms
- [ ] `features/auth/api.ts` — calls `AuthGateway` port
- [ ] `features/auth/components/LoginForm.tsx`
- [ ] `features/auth/components/RegisterForm.tsx`
- [ ] `app/(auth)/login/page.tsx`
- [ ] `app/(auth)/register/page.tsx`
- [ ] `core/services/HttpAuthGateway.ts` — concrete `AuthGateway` implementation
- [ ] Auth state context (`useAuth` hook) — stores display name / email from cookie-secured session

---

### Feature 1.2 — Categories

#### Backend
- [ ] `Category` domain model (Record: `id`, `name`, `parentId`, `icon`, `color`, `isSystem`, `ownerId`)
- [ ] `CategoryRepository` write port
- [ ] `CategoryReadRepository` read port (listByOwner — returns system categories + caller's own)
- [ ] `CreateCategoryCommand` + handler (block if `isSystem`; validate `parentId` max depth = 1)
- [ ] `UpdateCategoryCommand` + handler (block if `isSystem`)
- [ ] `DeleteCategoryCommand` + handler (block if `isSystem`; bulk-reassign transactions to "Outros" first; transaction is atomic — P7)
- [ ] `ListCategoriesQuery` + handler
- [ ] System categories seed — `ApplicationRunner` that upserts the 12 system categories on startup (idempotent)
- [ ] `CategoryDocument` + `CategoryMongoRepository`
- [ ] `CategoryController` — `GET /categories`, `POST /categories`, `PATCH /categories/{id}`, `DELETE /categories/{id}`
- [ ] `CategoryResponse` — omit `ownerId` from response (least disclosure, P6)
- [ ] `CategoryCommandHandlerTest` (unit) + `CategoryControllerIT` (integration) + tenancy isolation test

#### Frontend
- [ ] `features/categories/types.ts` — `Category`, `CreateCategoryRequest`, `UpdateCategoryRequest`
- [ ] `features/categories/api.ts` + `keys.ts` + hooks (`useCategories`, `useCreateCategory`, `useDeleteCategory`)
- [ ] `features/categories/components/CategoryPicker.tsx` — reusable selector used by transactions and budgets
- [ ] `features/categories/index.ts`

---

### Feature 1.3 — Accounts

Superseded by ADR-003 (see `docs/adr/ADR-003-materialized-derived-balances.md` and
`specs/003-accounts/`): `currentBalance` is a **materialized, stored** field kept in sync by an
async change-stream → SQS → projector pipeline, not a compute-on-read aggregation. The checklist
below reflects what's actually implemented; see `specs/003-accounts/implementation-notes.md` §11
for the full design and its own build-order checklist.

#### Backend
- [x] `Account` domain model (Record: `id`, `ownerId`, `name`, `type`, `institution`,
      `initialBalance`, `currentBalance`, `color`, `isActive`, `createdAt`, `version`)
- [x] `AccountType` enum (`CHECKING`, `SAVINGS`, `CASH`, `DIGITAL`)
- [x] `AccountRepository` write port
- [x] `AccountReadRepository` read port (`currentBalance` aggregation kept for backfill/reconcile use, `balanceHistory`)
- [x] `CreateAccountCommand` + handler (sets `currentBalance = initialBalance` at creation)
- [x] `UpdateAccountCommand` + handler
- [x] `DeactivateAccountCommand` / `ReactivateAccountCommand` + handlers (soft-delete: `isActive`)
- [x] `ReconcileAccountCommand` + handler — `ADJUST_INITIAL_BALANCE` sets `initialBalance` and
      `currentBalance` together; `ADJUSTING_TRANSACTION` rejected with 422 (deferred scope)
- [x] `ListAccountsQuery` / `GetAccountQuery` + handlers (return the materialized `currentBalance`)
- [x] `AccountDocument` (`currentBalance`, `@Version`) + `AccountMongoRepository`
- [x] `AccountBalanceChangeStreamListener` — publishes balance-change events to SQS on transaction writes
- [x] `ApplyAccountBalanceProjectionCommandHandler` (SQS consumer) — idempotent, transactional `$inc` of `currentBalance`
- [x] `ProjectionRepository` / `ProjectionCheckpointDocument` — change-stream resume-token checkpointing
- [x] `AccountController` — `GET /accounts`, `POST /accounts`, `GET /accounts/{id}`, `PATCH /accounts/{id}`,
      `DELETE /accounts/{id}` (soft), `POST /accounts/{id}/reactivate`, `POST /accounts/{id}/reconcile`,
      `GET /accounts/{id}/balance-history`
- [x] `AccountResponse` — includes `currentBalance` (materialized), omits `ownerId`
- [x] Unit tests per handler + `AccountIT`, `AccountRepositoryAdapterIT`, `AccountBalanceChangeStreamListenerIT`,
      `ProjectionCheckpointRepositoryAdapterIT` + cross-tenant isolation test
- [ ] `balance_snapshots` population (`BalanceSnapshotScheduler`) — schema exists, not built (explicitly deferred)
- [ ] `BalanceReconciliationJob` — not built (explicitly deferred)
- [ ] `ADJUSTING_TRANSACTION` reconciliation method — not built (explicitly deferred)

#### Frontend
- [x] `features/account/types.ts` — `Account` (with `currentBalance: Centavos`)
- [x] `features/account/api.ts` + `keys.ts` + hooks (`useAccounts`, `useCreateAccount`, `useUpdateAccount`,
      `useDeactivateAccount`, `useReactivateAccount`, `useReconcileAccount`, `useAccountBalanceHistory`)
- [x] `features/account/components/AccountList.tsx`
- [x] `features/account/components/AccountCard.tsx`
- [x] `features/account/components/AccountForm.tsx` (create/update)
- [x] `features/account/components/ReconcileAccountDialog.tsx`, `DeactivateAccountDialog.tsx`
- [x] `app/(app)/accounts/page.tsx`

---

## Phase 2 — Core Data Entry

**Spec files:** `specs/004-transactions`, `specs/005-cards`, `specs/006-import`

### Feature 2.1 — Transactions (Manual Entry)

#### Backend
- [ ] `Transaction` domain model (full field set per product definition; includes `sourceName` and `sourceType` — denormalized at write time, never updated)
- [ ] `PaymentMethod` enum, `TransactionType` enum, `ImportSource` enum
- [ ] `TransactionRepository` write port
- [ ] `TransactionReadRepository` read port (listByOwner with filters, findByIdAndOwner)
- [ ] `CreateTransactionCommand` + handler (account OR invoice, not both; assign to open invoice if credit card)
- [ ] `CreateRecurringTransactionCommand` + handler (generate instances up to 12 months ahead; `recurringSeriesId`)
- [ ] `CreateInstallmentTransactionCommand` + handler (N installments across N invoices; integer div + remainder to first)
- [ ] `CreateTransferCommand` + handler (atomic: 2-leg DEBIT + CREDIT, `transferPairId` idempotency, P7)
- [ ] `UpdateTransactionCommand` + handler (if recurring: delete + regenerate forward instances from `editedFromDate`)
- [ ] `DeleteTransactionCommand` + handler (if recurring: single or all-forward option)
- [ ] `ListTransactionsQuery` + handler (filters: dateRange, accountId, invoiceId, categoryId, paymentMethod, type, textSearch)
- [ ] `CategorySuggestionService` (keyword map from product definition)
- [ ] `TransactionDocument` + `TransactionMongoRepository`
- [ ] Budget alert trigger — after transaction save, check if any relevant budget crossed 80% or 100% threshold; emit `BudgetAlertEvent` (or direct call — defined in Phase 3, stub now)
- [ ] `TransactionController` — full CRUD + `GET /transactions/suggest-category?description={text}`
- [ ] `TransactionCommandHandlerTest` + `TransactionControllerIT` + tenancy isolation test + transfer atomicity test

#### Frontend
- [ ] `features/transactions/types.ts`
- [ ] `features/transactions/api.ts` + `keys.ts` + hooks
- [ ] `features/transactions/components/TransactionFeed.tsx` (global + per-account view)
- [ ] `features/transactions/components/TransactionRow.tsx` (date, description, category, amount, method tag)
- [ ] `features/transactions/components/AddTransactionModal.tsx` (manual entry form; tabs: simple / recurring / installment / transfer)
- [ ] `features/transactions/components/TransactionFilters.tsx`
- [ ] `app/(app)/transactions/page.tsx`

---

### Feature 2.2 — Cards & Invoices

#### Backend
- [ ] `CreditCard` domain model + `Invoice` domain model (per product definition)
- [ ] `CardType` enum, `InvoiceStatus` enum
- [ ] `CreditCardRepository` + `InvoiceRepository` write ports
- [ ] `CreditCardReadRepository` + `InvoiceReadRepository` read ports
- [ ] Invoice auto-generation — `InvoiceGenerationService`: for each CreditCard with no invoice for `targetMonth`, create one; called on card creation and on a monthly scheduled job
- [ ] `AssignTransactionToInvoiceService` — given a credit card transaction date, find (or create) the correct OPEN invoice
- [ ] `CreateCreditCardCommand` + handler (auto-generate current + next month invoices)
- [ ] `UpdateCreditCardCommand` + handler
- [ ] `DeactivateCreditCardCommand` + handler (soft-delete)
- [ ] `CloseInvoiceCommand` + handler (`OPEN → CLOSED`)
- [ ] `PayInvoiceCommand` + handler (atomic: `CLOSED → PAID` + DEBIT transaction on `associatedAccount`; `transferPairId`-like idempotency key)
- [ ] `ListInvoicesQuery` + handler (per card, per month range)
- [ ] `CreditCardDocument` + `InvoiceDocument` + Mongo repos
- [ ] `CreditCardController` — `GET /cards`, `POST /cards`, `GET /cards/{id}`, `PATCH /cards/{id}`, `DELETE /cards/{id}`
- [ ] `InvoiceController` — `GET /cards/{cardId}/invoices`, `GET /invoices/{id}`, `POST /invoices/{id}/close`, `POST /invoices/{id}/pay`
- [ ] Tests including invoice payment atomicity test + tenancy isolation

#### Frontend
- [ ] `features/cards/types.ts`
- [ ] `features/cards/api.ts` + `keys.ts` + hooks
- [ ] `features/cards/components/CardWallet.tsx` (CSS mockup cards)
- [ ] `features/cards/components/CardItem.tsx` (last 4, limit, utilization bar)
- [ ] `features/cards/components/InvoiceCycleMonitor.tsx` (tab strip: past 3 + current + next)
- [ ] `features/cards/components/InvoiceDetail.tsx` (status, amounts, transaction list)
- [ ] `features/cards/components/PayInvoiceModal.tsx`
- [ ] `app/(app)/cards/page.tsx`

---

### Feature 2.3 — Import (CSV + OFX)

#### Backend
- [ ] `ImportMapping` domain model — persisted per (ownerId + sourceName): column mapping config for CSV
- [ ] `ImportMappingRepository` write port + document
- [ ] `ParseImportCommand` (file bytes + format + sourceName) + handler — parse OFX or CSV, apply mapping, compute `importHash`/use FITID, return preview rows with dedup flags + category suggestions
- [ ] `ConfirmImportCommand` (list of approved row ids + category overrides) + handler — bulk-create transactions idempotently
- [ ] `OFXParser` service (parse SGML/XML OFX; extract FITID, date, amount, description)
- [ ] `CSVParser` service (parse with user-defined column mapping)
- [ ] Deduplication check — query existing `importHash` / `fitid` values for the owner before creating
- [ ] `ImportController` — `POST /import/parse` (multipart), `POST /import/mappings` (save mapping), `GET /import/mappings` (list), `POST /import/confirm`
- [ ] Tests including dedup test (re-importing same file produces no new transactions)

#### Frontend
- [ ] `features/import/types.ts`
- [ ] `features/import/api.ts` + hooks
- [ ] `features/import/components/ImportWizard.tsx` (step 1: upload; step 2: map columns if CSV; step 3: preview + adjust categories; step 4: confirm)
- [ ] `features/import/components/ImportPreviewTable.tsx` (shows parsed rows, duplicate flags, category selectors)
- [ ] `app/(app)/import/page.tsx`

---

## Phase 3 — Intelligence Layer

**Spec files:** `specs/007-dashboard`, `specs/008-budgets`

### Feature 3.1 — Dashboard

#### Backend
- [ ] `DashboardQuery` (ownerId, reference month/date) + `DashboardQueryHandler`
- [ ] Aggregation: Saldo Líquido — `SUM(account initialBalance + credits - debits) - SUM(open invoice totals)`
- [ ] Aggregation: KPI cards — current month CREDIT sum, DEBIT sum (excl. transfers + invoice payments), investment debits
- [ ] Aggregation: Cash flow chart — daily liquid balance for the requested period (7d / 30d / 3m / 12m)
- [ ] Aggregation: Expense distribution — current month DEBIT by top-level category (donut data)
- [ ] Aggregation: Radar de obrigações — faturas due ≤ 7 days + subscriptions `nextChargeDate` ≤ 7 days
- [ ] Aggregation: Budget progress — all budgets for current month with `spentAmount` (from Phase 3.2; can be stub returning empty list until budgets are built)
- [ ] `DashboardController` — `GET /dashboard`
- [ ] `DashboardResponse` record (all aggregated data)
- [ ] `DashboardAggregationIT`

#### Frontend
- [ ] `features/dashboard/types.ts`
- [ ] `features/dashboard/api.ts` + `keys.ts` + `useDashboard` hook
- [ ] `features/dashboard/components/SaldoLiquidoHero.tsx`
- [ ] `features/dashboard/components/KPICards.tsx`
- [ ] `features/dashboard/components/CashFlowChart.tsx` (Recharts `AreaChart` or `LineChart`, 4 range toggles)
- [ ] `features/dashboard/components/ExpenseDonut.tsx` (Recharts `PieChart`, expandable to subcategory)
- [ ] `features/dashboard/components/ObligationRadar.tsx` (urgency-colored list)
- [ ] `features/dashboard/components/BudgetProgressList.tsx`
- [ ] `app/(app)/dashboard/page.tsx` (or `/` redirect)

---

### Feature 3.2 — Budgets

#### Backend
- [ ] `Budget` domain model (Record: `id`, `ownerId`, `categoryId`, `month`, `limitAmount`, `isRecurring`, `alertAt80`, `alertAt100`)
- [ ] `BudgetRepository` write port
- [ ] `BudgetReadRepository` read port (listByOwnerAndMonth with `spentAmount` aggregation)
- [ ] `CreateBudgetCommand` + handler
- [ ] `UpdateBudgetCommand` + handler
- [ ] `DeleteBudgetCommand` + handler
- [ ] `ListBudgetsQuery` + handler — joins with transaction aggregation pipeline to compute `spentAmount`; includes all subcategory descendants (two-level hierarchy, single query)
- [ ] Monthly budget generation job — `@Scheduled` on first of month; for each `isRecurring` budget, create next month's instance (idempotent)
- [ ] `BudgetAlertService` — called from `TransactionCommandHandler`; checks if any active budget for the transaction's category + month crossed 80% or 100%; emits `BudgetAlertEvent` (stored for frontend polling or SSE)
- [ ] `BudgetDocument` + `BudgetMongoRepository`
- [ ] `BudgetController` — `GET /budgets?month={YYYY-MM}`, `POST /budgets`, `PATCH /budgets/{id}`, `DELETE /budgets/{id}`
- [ ] `BudgetActualsController` — `GET /budgets/actuals?month={YYYY-MM}` (all categories + budget vs actual table)
- [ ] `BudgetCommandHandlerTest` + `BudgetAggregationIT` + tenancy isolation test

#### Frontend
- [ ] `features/planning/types.ts` (budgets + goals shared types file)
- [ ] `features/planning/api.ts` + `keys.ts` + hooks (`useBudgets`, `useCreateBudget`, `useUpdateBudget`, `useDeleteBudget`, `useBudgetActuals`)
- [ ] `features/planning/components/BudgetCard.tsx` (progress bar, spent/limit, %)
- [ ] `features/planning/components/BudgetList.tsx` (month selector at top)
- [ ] `features/planning/components/CreateBudgetForm.tsx`
- [ ] `features/planning/components/BudgetActualsTable.tsx`
- [ ] `app/(app)/planning/page.tsx`

---

## Phase 4 — Planning & Portfolio

**Spec files:** `specs/009-goals`, `specs/010-investments`, `specs/011-subscriptions`

### Feature 4.1 — Goals (Cofres)

#### Backend
- [ ] `Goal` domain model (Record: `id`, `ownerId`, `name`, `targetAmount`, `currentAmount`, `deadline`, `linkedAccountId`, `icon`, `color`, `status`)
- [ ] `GoalStatus` enum (`IN_PROGRESS`, `COMPLETED`, `ABANDONED`)
- [ ] `GoalProgressEntry` — embedded list tracking manual progress updates with timestamps
- [ ] `GoalRepository` write port
- [ ] `GoalReadRepository` read port (listByOwner with projection calculations)
- [ ] `CreateGoalCommand` + handler
- [ ] `UpdateGoalProgressCommand` + handler (append entry; auto-transition to COMPLETED if `currentAmount >= targetAmount`)
- [ ] `UpdateGoalCommand` + handler (metadata updates)
- [ ] `DeleteGoalCommand` + handler
- [ ] `GoalProjectionService` — computes `requiredMonthly` (with deadline) or `projectedMonths` (without deadline, avg of last 3 updates)
- [ ] `GoalDocument` + `GoalMongoRepository`
- [ ] `GoalController` — `GET /goals`, `POST /goals`, `GET /goals/{id}`, `PATCH /goals/{id}`, `POST /goals/{id}/progress`, `DELETE /goals/{id}`
- [ ] `GoalCommandHandlerTest` + `GoalControllerIT` + tenancy isolation test

#### Frontend
- [ ] `features/planning/types.ts` — add `Goal` types
- [ ] Goal hooks (`useGoals`, `useCreateGoal`, `useUpdateGoalProgress`)
- [ ] `features/planning/components/GoalCard.tsx` (progress bar, projection, celebration state)
- [ ] `features/planning/components/GoalList.tsx`
- [ ] `features/planning/components/CreateGoalForm.tsx`
- [ ] `features/planning/components/UpdateGoalProgressModal.tsx`
- [ ] Extend `app/(app)/planning/page.tsx` with goals tab

---

### Feature 4.2 — Investments

#### Backend
- [ ] `Investment` domain model (full field set per product definition)
- [ ] `RateType` enum, `LiquidityType` enum, `InvestmentStatus` enum, `InvestmentType` enum
- [ ] `InvestmentValueEntry` — embedded list for value update history
- [ ] `InvestmentRepository` write port
- [ ] `InvestmentReadRepository` read port (portfolio summary aggregation + list)
- [ ] `CreateInvestmentCommand` + handler (optional: create linked DEBIT transaction)
- [ ] `UpdateInvestmentValueCommand` + handler (append to value history; recalculate IR estimate)
- [ ] `RedeemInvestmentCommand` + handler (status → REDEEMED; optional: create linked CREDIT transaction)
- [ ] `IRCalculationService` — table lookup + `(grossYield × irRate) / 10000` rule; returns 0 for `isIRExempt`
- [ ] `ListInvestmentsQuery` + handler (with IR + net value projections)
- [ ] Portfolio summary aggregation — total invested, gross value, estimated IR, net value, yield %, breakdown by type
- [ ] Maturity alert job — `@Scheduled` daily; investments maturing within 7 days emit `InvestmentMaturityAlertEvent`
- [ ] `InvestmentDocument` + `InvestmentMongoRepository`
- [ ] `InvestmentController` — `GET /investments`, `POST /investments`, `GET /investments/{id}`, `POST /investments/{id}/update-value`, `POST /investments/{id}/redeem`, `DELETE /investments/{id}`
- [ ] `InvestmentController` — `GET /investments/portfolio` (summary), `GET /investments/portfolio/history` (12-month monthly snapshots)
- [ ] `IRCalculationServiceTest` + `InvestmentControllerIT` + tenancy isolation test

#### Frontend
- [ ] `features/investments/types.ts`
- [ ] `features/investments/api.ts` + `keys.ts` + hooks
- [ ] `features/investments/components/PortfolioSummary.tsx` (total cards + type breakdown)
- [ ] `features/investments/components/InvestmentCard.tsx` (all calculated fields)
- [ ] `features/investments/components/InvestmentList.tsx`
- [ ] `features/investments/components/AddInvestmentForm.tsx`
- [ ] `features/investments/components/UpdateValueModal.tsx`
- [ ] `features/investments/components/RedeemModal.tsx`
- [ ] `app/(app)/investments/page.tsx`

---

### Feature 4.3 — Subscriptions

#### Backend
- [ ] `Subscription` domain model (with embedded `priceHistory: List<PriceEntry>` and optional `color` hex field)
- [ ] `PriceEntry` value object (Record: `amount`, `validFrom`)
- [ ] `BillingCycle` enum, `SubscriptionStatus` enum
- [ ] `SubscriptionRepository` write port
- [ ] `SubscriptionReadRepository` read port (list with monthly equivalent cost calculation)
- [ ] `CreateSubscriptionCommand` + handler (set `priceHistory` with initial entry)
- [ ] `UpdateSubscriptionCommand` + handler (append new `PriceEntry` if `amount` changed; update metadata)
- [ ] `PauseSubscriptionCommand` + `CancelSubscriptionCommand` + handlers
- [ ] `ListSubscriptionsQuery` + handler (with monthly equivalent cost computed)
- [ ] Cost summary aggregation — total monthly, total annual, biggest, by category
- [ ] `SubscriptionAutoDetectionService` — post-import scan: group transactions by (ownerId + description + amount); flag sequences at ~monthly intervals with ≥ 2 occurrences as suggestions
- [ ] Charge alert job — `@Scheduled` daily; subscriptions with `nextChargeDate` ≤ 3 days emit `SubscriptionChargeAlertEvent`
- [ ] `SubscriptionDocument` + `SubscriptionMongoRepository`
- [ ] `SubscriptionController` — full lifecycle CRUD + `GET /subscriptions/cost-summary`
- [ ] `SubscriptionCommandHandlerTest` + `CostSummaryAggregationIT` + tenancy isolation test

#### Frontend
- [ ] `features/subscriptions/types.ts`
- [ ] `features/subscriptions/api.ts` + `keys.ts` + hooks
- [ ] `features/subscriptions/components/SubscriptionList.tsx` (active/paused/cancelled toggle)
- [ ] `features/subscriptions/components/SubscriptionCard.tsx` (urgency indicator, monthly equivalent)
- [ ] `features/subscriptions/components/CostSummary.tsx` (total monthly/annual, by-category chart)
- [ ] `features/subscriptions/components/RenewalCalendar.tsx` (30-day timeline)
- [ ] `features/subscriptions/components/PriceHistoryTable.tsx`
- [ ] `features/subscriptions/components/AddSubscriptionForm.tsx`
- [ ] `app/(app)/subscriptions/page.tsx`

---

## In-App Notifications (alongside Phase 3+) — `specs/012-notifications`

- [ ] Backend: `Notification` document — `ownerId`, `type`, `message`, `read`, `payload`, `dedupeKey`, `createdAt`, `readAt`, `_version` (see `specs/012-notifications/data-model.md`)
- [ ] Backend: `NotificationRepository` + `NotificationController` (`GET /notifications`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all`)
- [ ] Backend: `NotificationService` — idempotent upsert on `{ownerId, dedupeKey}`; called by budget alert events on transaction save and by the daily scheduled jobs (invoice due ≤3d, subscription charge ≤3d, investment maturity ≤7d)
- [ ] Frontend: `features/notifications/` — `NotificationBell` (unread count badge from `unreadCount`), `NotificationPanel` (dropdown list)
- [ ] Frontend: toast integration — `NotificationProvider` dispatches toasts for real-time budget threshold alerts (80%/100%) on transaction save

---

## Constraints & Invariants (enforce at every PR)

| Rule | Where to check |
|---|---|
| Money is `Long` centavos — no `BigDecimal`/`double` | Domain models, commands, responses |
| Multiply before divide | Any percentage/IR calculation |
| `ownerId` from JWT only — never from request | `SecurityContext` → Reactor Context extraction |
| Every read scoped to `ownerId` | Read ports + query handlers |
| Not-owned resource → 404, not 403 | Command/query handlers |
| No derived value stored, **except** `currentBalance` (ADR-003: materialized via async projection) | `totalAmount`, `spentAmount`, "Saldo Líquido" |
| Multi-doc operations transactional (P7) | Transfers, invoice payment, category delete |
| Idempotent money commands | `transferPairId`, `importHash`, `FITID` |
| Responses omit `ownerId`, `passwordHash`, `@Version` | All `*Response` records |
| Schema-first — spec exists before implementation | PR review checklist |
