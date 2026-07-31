# PRD: Materialized Balance Projections

> **Document metadata**
> - **ID:** PRD-CROSS-01
> - **Status:** Approved (retroactive — written after `docs/adr/ADR-003-materialized-derived-balances.md`
>   and `docs/technical-solutions/materialized-projections.md` were already in place, to give this
>   change a BA-level companion document, same pattern as `PRD-004`/`PRD-005`)
> - **Author:** BA
> - **Last updated:** 2026-07-26
> - **Reviewers:** Lucas
> - **Linked Tech Spec:** `docs/technical-solutions/materialized-projections.md` (SPEC-CROSS-01)
> - **Linked ADR:** `docs/adr/ADR-003-materialized-derived-balances.md`

> **Why this PRD is cross-cutting, not per-feature:** this change touches how a value already
> promised by `PRD-003` (account balance) and a future value promised by `PRD-005` (invoice total)
> get produced — it doesn't add a user-facing feature of its own. It lives outside any single
> `specs/NNN-*` folder for the same reason the tech spec does. This document scopes only the
> account-balance half; the invoice-total half is out of scope (§8) until `005-cards` exists.

---

## 1. Problem Statement

Mithril Vault's entire value proposition is a trustworthy, current picture of the user's money.
`PRD-003` already promises "current balance" as a first-class number on every account card; the
dashboard (`007-dashboard`) promises the same for a household-wide "Saldo Líquido Disponível."
Both are currently produced by re-reading and re-summing **every transaction an account has ever
had**, on every single request — there is no caching, no bound, nothing that stops that scan from
growing as the user's transaction history grows. For a personal-finance app whose entire pitch is
"you can trust this number, and see it instantly," a balance computation that gets slower the
longer someone uses the app is a problem baked into the foundation, not a later scaling exercise.

This app is intentionally built multi-user even though it has exactly one real user today (see
`docs/architecture-contract.md` P2) — a deliberate learning exercise in building things the way a
real product would need to work, not the way a one-person tool minimally requires. The balance
computation is the clearest case where "minimally sufficient for one user" and "how a real
financial product would actually do this" diverge: every real ledger/banking system (bank
statements, Stripe-style account layers) keeps a running balance rather than re-deriving it from
full history on every read, specifically because the naive approach doesn't hold up. This change
brings that same discipline here, before the app has a scaling problem to react to rather than
after.

---

## 2. Goals

- [x] Make reading an account's current balance a fast, constant-time operation, regardless of how
      many transactions that account has accumulated.
- [x] Make balance correctness self-recovering: if the tracked balance ever drifts from what the
      transaction history actually says, the system detects and corrects it without a person
      noticing a wrong number first. *(Detection/self-heal mechanism — `BalanceReconciliationJob`
      — is designed but explicitly not built in this delivery; see §8 and the Appendix.)*
- [x] Fix the pre-existing gap where every account read (`create`, `update`, `get`, `list`,
      `reactivate`, `reconcile`) was silently echoing the account's *initial* balance instead of a
      real computed one — this PRD's delivery is also the first time `currentBalance` is a real,
      correct number anywhere in the product.
- [x] Establish one reusable pattern so the same problem (a number that must stay in sync with an
      ever-growing transaction history) doesn't get re-solved from scratch for invoice totals,
      budget spending, or the dashboard's household total later.
- [x] Do this using infrastructure the project already committed to (`ADR-002`'s SQS/LocalStack
      setup for invoice generation) rather than introducing a second, unrelated async mechanism —
      one way async side effects get processed in this codebase, not two.

### Non-Goals

- Not in scope: invoice total (`invoices.totalAmount`) or credit-card utilized-amount projections
  — those belong to `005-cards`, which hasn't been built yet (see §8).
- Not in scope: the scheduled snapshot job (`BalanceSnapshotScheduler`) and the scheduled
  self-healing reconciliation job (`BalanceReconciliationJob`) — designed in the tech spec, not
  implemented in this delivery. Tracked as immediate follow-up work, not a future "someday."
- Not in scope: any user-visible "balance is updating" indicator in the UI. The eventual-
  consistency window is sub-second in practice; adding UI to represent a delay the user is very
  unlikely to ever observe is not worth the design/build cost right now (see Appendix Q1).
- Not in scope: budget `spentAmount` or dashboard "Saldo Líquido" adopting the same pattern —
  future work, once those features exist, reusing this design without re-deciding it.

---

## 3. Users and Context

| Segment | Description | Volume / Impact |
|---|---|---|
| Owner | The one person whose finances this instance tracks (app is scoped multi-user for learning purposes, but every account/transaction is owner-scoped) | High — every account card and the dashboard depend on this being correct and fast |
| System (background) | The change-stream listener and SQS consumer that keep the balance in sync; not a human-facing actor, but its failure modes are user-visible (a wrong or stale balance) | Internal, but directly determines the product's core trust promise |

---

## 4. Use Cases

### UC-001: View an account's current balance

**Actor:** Owner
**Goal:** See an accurate, current balance for an account without a noticeable delay
**Precondition:** The account exists and belongs to the owner; it may have any amount of
transaction history, from none to years of activity

1. Owner opens the accounts screen (or any screen showing an account card).
2. System returns the account's current balance as a single fast lookup, not a scan over that
   account's transaction history.
3. The number reflects every transaction posted against the account up to a very recent point —
   in practice, the balance shown either already includes a transaction created moments ago, or
   will on the very next read; there is never a case where it's permanently wrong for a
   successfully-created transaction.

**Alternate path — balance drift (future, see §8):** if the tracked balance and the true
transaction-derived balance ever disagree (a bug, a missed event), the system notices during its
next reconciliation cycle and corrects itself, logging the discrepancy for later review — the
owner never has to notice or report a wrong number for it to get fixed. *(Job not built in this
delivery — see Appendix Q2 for interim mitigation.)*

### UC-002: Reconcile an account against a real-world statement (unaffected, must keep working)

**Actor:** Owner
**Goal:** Correct the app's tracked balance to match a real bank statement
**Precondition:** Account exists; owner knows the real current balance

1. Owner enters the real balance via the existing reconcile flow (`PRD-003`, "direct adjustment"
   method).
2. System updates the account so its tracked balance now equals the real balance the owner
   entered, immediately and consistently — not eventually.

This use case already exists (`PRD-003`); it's listed here only because this delivery changes
*how* the account's balance is stored, and reconciliation must not regress as a result — see
FR-004.

---

## 5. Functional Requirements

| ID | Priority | Description |
|---|---|---|
| FR-001 | P0 | The system shall return an account's current balance as a fast, single-record lookup, with no request-time computation over that account's transaction history. |
| FR-002 | P0 | The system shall update an account's tracked balance to reflect a newly created transaction without requiring the owner to take any action beyond the normal transaction-creation flow. |
| FR-003 | P0 | The system shall never lose or silently skip a balance update for a successfully created transaction — every transaction that exists eventually (and in practice, near-instantly) reflects in its account's balance. |
| FR-004 | P0 | The system shall continue to support direct-adjustment reconciliation (entering a known real balance), with the result taking effect immediately, not eventually. |
| FR-005 | P1 | The system shall, on detecting a discrepancy between the tracked balance and the true transaction-derived balance, correct it automatically and record that a correction occurred. *(Design complete, build deferred — see §8.)* |
| FR-006 | P1 | The mechanism used to keep an account's balance in sync shall be reusable, without a fresh design, for a credit card invoice's total once that feature is built. |

---

## 6. Non-Functional Requirements

| ID | Category | Description |
|---|---|---|
| NFR-001 | Performance | Reading an account's current balance shall not get slower as that account accumulates more transaction history. |
| NFR-002 | Consistency | A balance update following a new transaction shall typically complete in well under one second, and shall never be indefinitely delayed. |
| NFR-003 | Reliability | A transient failure while processing a balance update shall not permanently lose that update — the system shall retry until it succeeds, without double-applying it if the retry is itself a duplicate. |
| NFR-004 | Auditability | Every change to a tracked balance shall be traceable back to the specific transaction (or reconciliation action) that caused it; the underlying transaction history is never modified in place. |
| NFR-005 | Operability | The asynchronous mechanism used to keep balances in sync shall reuse existing project infrastructure (the SQS/LocalStack setup already committed to for invoice generation) rather than introducing a second, unrelated mechanism. |
| NFR-006 | Scale (stated learning goal) | The design shall not assume a small, fixed number of accounts or a small, fixed amount of transaction history per account, consistent with this project's stated multi-user, over-engineered-on-purpose scope (`docs/architecture-contract.md` P2). |

---

## 7. Acceptance Criteria

| ID | Linked Requirements | Criterion |
|---|---|---|
| AC-001 | FR-001, NFR-001 | Given an account with a large amount of transaction history, when its balance is read, then the response does not involve scanning that history — response time does not meaningfully increase as history grows. |
| AC-002 | FR-002, NFR-002 | Given an account with an existing balance, when a new transaction is created against it, then the account's balance reflects that transaction within, at most, a few seconds — typically far sooner. |
| AC-003 | FR-003, NFR-003 | Given the same balance-update event is processed more than once (e.g. due to a retry), when both are applied, then the balance reflects the transaction exactly once, never twice. |
| AC-004 | FR-004 | Given an owner reconciles an account with a real balance, when the reconciliation is submitted, then the account immediately shows that exact balance — not eventually. |
| AC-005 | NFR-005 | Given the project's existing SQS infrastructure, when this delivery's async balance-sync mechanism is built, then it uses that same queue/consumer/dead-letter-queue shape rather than a second messaging system. |

---

## 8. Out of Scope

- Invoice total (`invoices.totalAmount`) and credit-card utilized-amount projections — `005-cards`,
  not yet built.
- `BalanceSnapshotScheduler` and `BalanceReconciliationJob` — designed (tech spec §3.3–3.4), not
  implemented in this delivery. **This is the one gap worth a stakeholder's attention:** until the
  reconciliation job exists, a balance-update failure that happens to occur *after* the event is
  marked processed (a narrow crash window — see the tech implementation notes,
  `specs/003-accounts/implementation-notes.md` §11.6, for the exact mechanics and how most of this
  window is closed by wrapping the update in a database transaction) would go uncorrected until
  the reconciliation job is eventually built. This is a known, accepted, and narrow gap for this
  delivery, not an oversight.
- Budget `spentAmount` and dashboard "Saldo Líquido" reusing this pattern — future work.
- Any user-facing indication that a balance update is "in progress."

---

## Appendix: Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Is a "balance updating" UI affordance ever worth building, or does the sub-second window make it permanently not worth the cost? | BA + Frontend | Open — leaning "never," revisit only if real user reports ever surface confusion. |
| 2 | Given the reconciliation job (FR-005) isn't built yet, is there an interim mitigation worth doing now (e.g. a manual admin/debug trigger to re-run the ground-truth computation for one account on demand) versus just accepting the narrow gap until the job is built? | Architect | Open — same question as SPEC-CROSS-01 Appendix Q2. |
| 3 | When `BalanceReconciliationJob` is eventually built, should a nonzero drift rate be surfaced anywhere the owner can see it (a "last verified" timestamp on the account card), or is it purely an internal signal? | BA | Open. |
