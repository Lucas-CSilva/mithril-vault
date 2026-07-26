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
| `currentBalance` | Int64 | Centavos. Materialized projection, kept in sync by `AccountBalanceProjector`. See [Derived Values](#derived-values-materialized-projections). |
| `color` | String | Hex color, e.g. `#88C0D0` |
| `isActive` | Boolean | Soft-delete flag. Inactive accounts are hidden in the UI but retained. |
| `createdAt` | Date | UTC instant |
| `_version` | Int64 | Optimistic locking (`@Version`). Concurrent edits use this field; also guards the reconciliation job's self-heal writes to `currentBalance`. |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Tenant-scoped listing (required by P2) |
| `{ ownerId: 1, name: 1 }` | Unique | collation `{ locale: "pt", strength: 2 }` | Prevent duplicate account names per user |

---

## Derived Values (materialized projections)

> Per `docs/adr/ADR-003-materialized-derived-balances.md` (supersedes P4 of
> `docs/architecture-contract.md`), `currentBalance` is a **stored, materialized field**, not
> computed at read time. This section defines the value; the ADR defines *why*; the technical
> solution at `docs/technical-solutions/materialized-projections.md` defines *how it's built*
> (component breakdown, sequence diagrams, rollout order).

### `currentBalance`

**Definition** (unchanged from the original derivation — this is the invariant the materialized
field must always converge to):

```
currentBalance = initialBalance
              + SUM(amount WHERE type = CREDIT AND accountId = this._id)
              - SUM(amount WHERE type = DEBIT  AND accountId = this._id)
```

**How it's kept in sync:** `AccountBalanceProjector` subscribes to a MongoDB Change Stream on
`transactions` and applies each insert/reversal as an atomic `$inc` on `accounts.currentBalance`.
Reads are a plain document fetch — no aggregation on the request path. The original aggregation
pipeline (`$match` + `$group` + `$sum` over `transactions`) is retained, scoped by the latest
`balance_snapshots` entry, as `recomputeBalance` — used only by the reconciliation job, the
one-time backfill, and admin tooling, never by `GET /accounts`. Full pattern (checkpointing,
idempotency guard, snapshot cadence, reconciliation/self-heal) in ADR-003.

### `balance_snapshots` (new collection)

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users |
| `accountId` | String (UUID) | FK → accounts |
| `asOfDate` | Date | Snapshot checkpoint date |
| `balance` | Int64 | Centavos. `currentBalance` as of `asOfDate`. |
| `throughTransactionId` | String (UUID) | Last transaction included in this snapshot's sum |

Written on a schedule (e.g. monthly). Bounds `recomputeBalance` to "latest snapshot ≤ date +
transactions since," instead of scanning full account history.

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

1. **Adjusting transaction path:** create a `CREDIT` or `DEBIT` transaction with `description = "Reconciliação"` and a flag `isReconciliation = true`. This transaction is shown distinctly in the feed, and flows through `AccountBalanceProjector` like any other transaction — no separate handling needed.
2. **Direct adjustment path:** update `initialBalance` **and** `currentBalance` together in the same document update, so the materialized field stays correct even though no transaction (and therefore no projector event) is involved. This is the one application-code write path allowed to touch `currentBalance` directly, alongside the projector and reconciliation job (ADR-003's "one owner" rule names all three explicitly).

Both paths result in the same `currentBalance`. The user chooses which path via the UI.
