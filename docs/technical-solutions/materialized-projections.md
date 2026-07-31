# Technical Specification: Materialized Derived-Value Projections

> **Document metadata**
> - **ID:** SPEC-CROSS-01
> - **Status:** Approved
> - **Author:** Architect
> - **Last updated:** 2026-07-27 (added `ProjectionLeaderElector` — multi-instance safety for the
>   change-stream listener, closes Appendix Q6 below)
> - **Linked PRD:** `docs/prd/PRD-CROSS-01-materialized-balance-projections.md` — PRD-CROSS-01
>   (account-balance half only; invoice-total half has no PRD yet, pending `005-cards`)
> - **Linked ADR:** `docs/adr/ADR-003-materialized-derived-balances.md`
> - **Linked Specs:** `specs/003-accounts/data-model.md`, `specs/005-cards/data-model.md`,
>   `specs/007-dashboard/data-model.md`
> - **Reviewers:** Lucas

---

## 1. Overview

This is a **cross-cutting** technical spec, not a per-feature one — it doesn't belong to a single
`specs/NNN-*` folder because it's a pattern shared by `003-accounts` (retrofit onto an existing,
shipped aggregation) and `005-cards` (built in from day one, since that feature has no
implementation yet). It documents how `accounts.currentBalance` and `invoices.totalAmount` go
from "computed fresh on every read" to "stored fields kept in sync by an event-driven projector,"
per `docs/adr/ADR-003-materialized-derived-balances.md`. That ADR records *why*; this document
records *how it's built*: the components, their sequencing, and the failure modes each one exists
to close.

Deliberately over-scoped as a learning exercise (one real user, multi-user architecture is a
stated goal — `docs/architecture-contract.md` P2) — the same framing `ADR-002` used for
event-driven invoice generation. The two documents share a lesson: **reconciliation, not
delivery guarantees, is what makes an async system trustworthy.** ADR-002 leans on SQS
redelivery + a self-deriving monthly job; this design leans on the same SQS/`@SqsListener`/DLQ
machinery ADR-002 already established, fed by a MongoDB Change Stream that guarantees no write to
`transactions` is ever missed, plus idempotent projection and a self-healing reconciliation job.

**Why a Change Stream *and* SQS, not just one:** the Change Stream is what detects every write to
`transactions` regardless of which code path produced it — nothing can forget to trigger the
projection, because nothing has to remember to call anything; the change stream just observes the
oplog. SQS is what actually delivers that event to the component that applies the `$inc`, reusing
the exact queue/listener/DLQ shape ADR-002 already built for invoice generation instead of running
a second, structurally-identical async mechanism in this codebase. See ADR-003's Decision and
Alternatives (Option A-revised, Option B) for the full reasoning.

**Architecture diagram:**

```mermaid
flowchart LR
  subgraph Write["Write path (unchanged)"]
    TxnHandler["CreateTransactionCommandHandler"]
    TxnRepo["TransactionRepositoryAdapter"]
  end

  subgraph Log["Event log"]
    Txns[("MongoDB\ntransactions")]
  end

  subgraph Trigger["Trigger (infrastructure)"]
    ChangeStream[["Change Stream\non transactions"]]
    Listener["AccountBalanceChangeStreamListener"]
    Checkpoints[("projection_checkpoints")]
  end

  subgraph Transport["Transport"]
    Queue[["SQS\nmithril-vault-balance-projection"]]
    DLQ[("DLQ")]
  end

  subgraph Consumers["Consumers (infrastructure, @SqsListener)"]
    AcctProj["AccountBalanceProjector"]
    InvProj["InvoiceTotalProjector"]
  end

  subgraph ReadModels["Materialized read models"]
    Accounts[("accounts.currentBalance")]
    Invoices[("invoices.totalAmount")]
  end

  subgraph Reconciliation["Reconciliation (infrastructure)"]
    Recon["BalanceReconciliationJob"]
    Snapshots[("balance_snapshots\ninvoice_total_snapshots")]
  end

  TxnHandler --> TxnRepo --> Txns
  Txns --> ChangeStream
  ChangeStream --> Listener
  Listener -.checkpoint.-> Checkpoints
  Listener -->|"publish\nBalanceProjectionMessage"| Queue
  Queue --> AcctProj
  Queue --> InvProj
  Queue -.failed deliveries.-> DLQ
  AcctProj --> Accounts
  InvProj --> Invoices

  Recon --> Txns
  Recon --> Snapshots
  Recon -->|"compare + self-heal"| Accounts
  Recon -->|"compare + self-heal"| Invoices
```

---

## 2. Architecture

### 2.1 Key Architectural Decisions

- **MongoDB Change Stream as trigger, SQS as transport** — the Change Stream subscribes directly
  to `transactions` so no write is ever missed regardless of which code path produced it; it
  publishes to the same SQS/`@SqsListener`/DLQ shape ADR-002 already established for invoice
  generation, rather than applying the `$inc` inline (Option A-revised, rejected) or introducing a
  hand-rolled outbox collection (Option B, superseded — see ADR-003 Alternatives for both).
- **Idempotent, at-least-once projection** — both the Change Stream (on listener restart) and SQS
  (on redelivery) can redeliver; every applied event marks itself on the source transaction
  (`appliedProjections`) so a replay from either leg is a no-op, not a double-count.
- **Order-independence is load-bearing, not incidental** — SQS standard queues don't guarantee
  order. This design is safe only because `$inc` is commutative; a future projection with
  order-sensitive semantics would need a different transport (see ADR-003's "Out-of-order
  delivery" failure mode).
- **Reconciliation as the resilience mechanism** — same shape as ADR-002's monthly rollover job:
  a scheduled process re-derives ground truth independently and self-heals drift, rather than the
  system trusting that projection delivery was perfect.
- **Snapshots bound recomputation, not just storage** — `balance_snapshots` /
  `invoice_total_snapshots` exist so reconciliation and any future "balance as of date" query
  never re-scan full transaction history, only "since the last checkpoint."
- **One owner per materialized field** — only the `@SqsListener` projector, the reconciliation
  job, and (for accounts only) the reconcile-account direct-adjustment path may write
  `currentBalance` / `totalAmount`. No other application code touches these fields (ADR-003,
  restated from P4).
- **At most one active change-stream subscriber, cluster-wide** — under a horizontally-scaled
  deployment (e.g. multiple k8s pods), every replica would otherwise open its own Change Stream
  against `transactions` and race on the same `projection_checkpoints` row (last-writer-wins
  corruption of the resume token) and double-publish every event. `ProjectionLeaderElector`
  (§3.1a) makes exactly one instance hold an active subscription at a time, with automatic
  failover on crash — added once a real multi-pod deployment was planned; see Appendix Q6.

### 2.2 Design Patterns Used

| Pattern | Applied where | Rationale |
|---|---|---|
| Event-driven projection (CQRS read model) | `AccountBalanceProjector`, `InvoiceTotalProjector` | Decouples the transaction write path from every derived value that must update; matches the account-layer/general-ledger split real ledger cores use |
| Change Stream as durable trigger | `AccountBalanceChangeStreamListener` | Guarantees every `transactions` write fires the projection — no application code has to remember to publish anything |
| Queue as transport (reused from ADR-002) | SQS `mithril-vault-balance-projection`, `@SqsListener` consumers | One "how async side effects get applied" mechanism for the whole codebase instead of two; gets fan-out/DLQ for free |
| Idempotent consumer | Projectors, guarded by `appliedProjections` marker | Both change-stream replay and SQS redelivery are at-least-once; a replay finds itself already applied and no-ops |
| Checkpointing | `projection_checkpoints` | Resumes the change-stream listener after restart without replaying from the beginning of the collection |
| Snapshot / checkpoint pattern | `balance_snapshots`, `invoice_total_snapshots` | Bounds any full recomputation to "since last snapshot," the same technique bank statements use |
| Reconciliation job | `BalanceReconciliationJob` | Re-derives ground truth every cycle and self-heals — tolerant of projector bugs or missed events by construction, same shape as `InvoiceRolloverScheduler` (ADR-002) |
| Optimistic concurrency | `_version` on `accounts`/`invoices` | Prevents the reconciliation job's self-heal write from racing a concurrently-applied projector `$inc` |
| Heartbeat-lease leader election | `ProjectionLeaderElector` | Guarantees a single active change-stream subscriber cluster-wide; not ShedLock (see §3.1a) because the subscription is held indefinitely, not executed once per bounded interval |

---

## 3. Component / Service Breakdown

### 3.1 `AccountBalanceChangeStreamListener`

**Responsibility:** Detect every write to `transactions` and publish a `BalanceProjectionMessage`
for it. Owns no materialized field itself — it is the trigger, not the writer.

**Exposes:** no public API — a background subscriber, started on application boot.

**Depends on:** the `transactions` Change Stream, `projection_checkpoints`, the
`mithril-vault-balance-projection` SQS queue.

**Message shape:** `BalanceProjectionMessage(ownerId, transactionId, accountId, invoiceId, type,
amount, target)` — `target` is `ACCOUNT` or `INVOICE`, letting one queue and one listener serve
both `AccountBalanceProjector` and `InvoiceTotalProjector`, the same "one message shape, multiple
consumers care about a field of it" instinct ADR-002 used for `GenerateInvoiceMessage`. Note this
means a single `@SqsListener` entry point dispatches on `target` to the two projector services —
SQS queues are competing-consumer, not pub/sub, so two independent `@SqsListener` methods both
bound to this queue would race for every message rather than each reliably seeing only its own
`target`.

### 3.1a `ProjectionLeaderElector` — multi-instance safety

**Responsibility:** Ensure at most one application instance holds an active subscription to the
`transactions` change stream at any time, and that a crashed leader is automatically replaced.

**Why not ShedLock** (already used for `BalanceSnapshotScheduler`/`BalanceReconciliationJob`, §3.3–3.4):
ShedLock's model is a bounded lock (`lockAtMostFor`) held around one execution of a `@Scheduled`
method, then released. The change-stream listener isn't a discrete execution — it's a subscription
meant to be held indefinitely, with hand-off only on crash. A heartbeat-lease pattern (continuously
renew a short-TTL lock while alive) fits that shape; forcing it into ShedLock's bounded-lock model
would mean either constant re-acquisition (defeating the point of a long-lived subscription) or an
artificially large `lockAtMostFor` that delays failover by that same duration.

**Mechanism:** a new single-document-per-key collection `projection_leases`
(`projectionName`, `instanceId`, `leaseExpiresAt`), acquired/renewed via an atomic `findAndModify`
upsert (`instanceId == self OR leaseExpiresAt < now`). The listener composes its change-stream
`Flux` behind a `leadershipSignal().switchMap(isLeader -> isLeader ? stream : Flux.empty())` —
losing the lease disposes the inner subscription automatically via Reactor's `switchMap` contract,
so no separate shutdown/hand-off logic is needed. Suggested lease TTL 30s, renewed every 10s.

**Exposes:** no public API — an internal collaborator of `AccountBalanceChangeStreamListener`.

**Depends on:** `projection_leases` (new collection, infra-only, same tier as `projection_checkpoints`).

### 3.2 `AccountBalanceProjector` / `InvoiceTotalProjector`

**Responsibility:** Consume `BalanceProjectionMessage` and apply it as an atomic, idempotent
increment to the owning account's `currentBalance` (`target = ACCOUNT`) or invoice's `totalAmount`
(`target = INVOICE`).

**Exposes:** no public API — `@SqsListener` consumers on `mithril-vault-balance-projection`,
analogous to ADR-002's invoice-generation consumer, just filtering/branching on `target`.

**Depends on:** the SQS queue, the target collection (`accounts` or `invoices`).

**Happy-path flow (transaction write → projection applied):**

```mermaid
sequenceDiagram
  autonumber
  actor Owner
  participant API as "TransactionController"
  participant Handler as "CreateTransactionCommandHandler"
  participant TxnRepo as "TransactionRepositoryAdapter"
  participant Mongo as "MongoDB (transactions)"
  participant Stream as "Change Stream"
  participant CSListener as "AccountBalanceChangeStreamListener"
  participant CP as "projection_checkpoints"
  participant Queue as "SQS (balance-projection)"
  participant Proj as "AccountBalanceProjector"
  participant Acct as "MongoDB (accounts)"

  Owner->>API: "POST /transactions"
  API->>Handler: CreateTransactionCommand
  Handler->>TxnRepo: save(transaction)
  TxnRepo->>Mongo: insert
  Mongo-->>TxnRepo: acknowledged
  TxnRepo-->>Handler: Transaction
  Handler-->>API: Transaction
  API-->>Owner: "201 Created"
  Mongo-)Stream: change event (insert)
  Stream->>CSListener: deliver event
  CSListener->>Queue: publish BalanceProjectionMessage
  CSListener->>CP: advance resumeToken
  Queue->>Proj: deliver message
  Proj->>Mongo: "findOneAndUpdate WHERE appliedProjections excludes accountBalance"
  alt not yet applied
    Mongo-->>Proj: matched, marked applied
    Proj->>Acct: "$inc currentBalance"
  else already applied (replay or redelivery)
    Mongo-->>Proj: no match — no-op
  end
```

Note the response returns at step 8, before either the change-stream listener or the SQS consumer
has run — this is the (now two-hop) eventual-consistency window ADR-003 calls out explicitly
(Consequences, "Negative").

**Restart / idempotent replay flow — two independent redelivery sources:**

```mermaid
sequenceDiagram
  autonumber
  participant CSListener as "AccountBalanceChangeStreamListener"
  participant CP as "projection_checkpoints"
  participant Stream as "Change Stream"
  participant Queue as "SQS (balance-projection)"
  participant Proj as "AccountBalanceProjector"
  participant Mongo as "MongoDB (transactions)"

  Note over CSListener: "instance restarts (deploy or crash)"
  CSListener->>CP: read last resumeToken
  CP-->>CSListener: "resumeToken (may be slightly stale)"
  CSListener->>Stream: "resume(resumeToken)"
  Stream-->>CSListener: replay events since token (at-least-once)
  loop for each replayed event
    CSListener->>Queue: publish BalanceProjectionMessage
  end
  Note over Queue,Proj: "separately: SQS itself may redeliver an already-consumed message\n(visibility timeout expiry, consumer crash before ack)"
  loop for each message received (replayed OR redelivered)
    Proj->>Mongo: "findOneAndUpdate WHERE appliedProjections excludes accountBalance"
    alt already applied
      Mongo-->>Proj: no match — no-op, no double count
    else not yet applied
      Mongo-->>Proj: matched — apply $inc, mark applied
    end
  end
```

This is the diagram that justifies the idempotency guard's existence: without the
`appliedProjections` check, either redelivery source alone — a replayed change-stream event or a
redelivered SQS message — would double-count.

### 3.3 `BalanceSnapshotScheduler` / `InvoiceTotalSnapshotScheduler`

**Responsibility:** Periodically (e.g. monthly) write a `balance_snapshots` /
`invoice_total_snapshots` document per account/invoice, bounding how far back
recomputation ever needs to scan.

**Exposes:** a single `@Scheduled` method each, no public API — same shape as
`InvoiceRolloverScheduler` (ADR-002), including a ShedLock guard so only one instance fires per
cycle in a horizontally-scaled deployment.

**Depends on:** the *ground-truth* aggregation (the original `$match` + `$group` + `$sum`
pipeline this whole design replaces as the request-serving path — kept alive here as
`recomputeBalance`/its invoice equivalent), scoped by the previous snapshot.

### 3.4 `BalanceReconciliationJob` / `InvoiceTotalReconciliationJob`

**Responsibility:** Catch drift between the materialized field and ground truth; self-heal.

**Exposes:** a single `@Scheduled` method each (e.g. nightly), ShedLock-guarded.

**Depends on:** `balance_snapshots` (or `invoice_total_snapshots`), the ground-truth aggregation,
the materialized field, `_version` for the guarded self-heal write.

**Reconciliation / self-heal flow:**

```mermaid
sequenceDiagram
  autonumber
  participant Cron as "Scheduler (cron trigger)"
  participant Recon as "BalanceReconciliationJob"
  participant Snap as "balance_snapshots"
  participant Txns as "MongoDB (transactions)"
  participant Acct as "MongoDB (accounts)"

  Cron->>Recon: fire (nightly)
  loop for each account (bounded concurrency)
    Recon->>Snap: latest snapshot before today
    Snap-->>Recon: "{asOfDate, balance}"
    Recon->>Txns: sum(amount) since snapshot
    Txns-->>Recon: delta
    Recon->>Recon: "groundTruth = snapshot.balance + delta"
    Recon->>Acct: read currentBalance
    Acct-->>Recon: materialized value
    alt groundTruth != materialized
      Recon->>Acct: "self-heal: set currentBalance = groundTruth (guarded by _version)"
      Recon->>Recon: log drift + increment metric
    else values match
      Recon->>Recon: no-op
    end
  end
```

The same flow applies to `InvoiceTotalReconciliationJob` against `invoice_total_snapshots` and
`invoices.totalAmount`.

### 3.5 Package placement (hexagonal architecture)

Following the shape ADR-002/`005-cards/technical-solution.md` already established for
scheduler/listener-style inbound triggers:

| Component | Package | Rationale |
|---|---|---|
| `AccountBalanceChangeStreamListener` | `infrastructure/adapter/projection` | Framework/Mongo-specific (Change Streams API) — the trigger, not a port implementation |
| `AccountBalanceProjector`, `InvoiceTotalProjector` | `infrastructure/adapter/projection` | `@SqsListener` consumers, same tier as `GenerateInvoiceListener` (ADR-002) |
| `BalanceProjectionMessage`, `BalanceProjectionQueuePublisher` | `infrastructure/adapter/messaging` (resolved — new package, not yet present when this spec was written) | Isolates the `SqsTemplate`/AWS SDK type from the listener; sets up a home for ADR-002's own invoice-generation publish call to move into for parity, once built |
| `projection_checkpoints` access | `infrastructure/adapter/persistence` | Plain Mongo repository, no domain logic — `ProjectionCheckpointRepositoryAdapter`, same `*RepositoryAdapter` shape as `AccountRepositoryAdapter` |
| `ProjectionLeaderElector` (+ `projection_leases` access) | `infrastructure/adapter/projection` | Framework/Mongo-specific, tightly coupled to the change-stream listener it gates; not a general-purpose repository |
| `BalanceSnapshotScheduler`, `BalanceReconciliationJob` (+ invoice equivalents) | `infrastructure/scheduler` (alongside `InvoiceRolloverScheduler`) | Timer-driven inbound triggers, not HTTP-facing, same tier as the existing scheduler |
| `recomputeBalance` ground-truth aggregation | `domain/port` (`AccountReadRepository`) + `infrastructure` impl | Unchanged location — it's the same aggregation that already lived there, just no longer called from the request path |

---

## 4. Data Model

Field-level schema for `accounts`/`invoices`/`balance_snapshots`/`invoice_total_snapshots` is
defined in `specs/003-accounts/data-model.md` and `specs/005-cards/data-model.md` — this section
only adds the cross-cutting piece those specs don't own: `projection_checkpoints`, and how
everything relates.

### 4.1 `projection_checkpoints` (new collection, infra-only — no `ownerId`)

| Field | BSON Type | Notes |
|---|---|---|
| `_id` / `projectionName` | String | e.g. `"accountBalance"`, `"invoiceTotal"` — primary key |
| `resumeToken` | Document | Opaque MongoDB change-stream resume token |
| `lastProcessedTransactionId` | String (UUID) | Last transaction this projection applied, for observability |
| `updatedAt` | Date | UTC instant |

One document per **change-stream listener** (not per `@SqsListener` consumer — SQS's own
redelivery/visibility-timeout mechanics cover the listener→consumer leg; this checkpoint only
protects the Change Stream→publish leg from replaying since the beginning of the collection),
analogous to how `shedLock` (ADR-002) is infra-only shared state with no tenant data.

### 4.1a `projection_leases` (new collection, infra-only — no `ownerId`)

| Field | BSON Type | Notes |
|---|---|---|
| `_id` / `projectionName` | String | e.g. `"accountBalance"` — same key space as `projection_checkpoints`, separate collection |
| `instanceId` | String | Current lease holder, e.g. `hostname-<uuid>` generated once per instance at startup |
| `leaseExpiresAt` | Date | UTC instant; renewed continuously by the holder, expiry is what allows failover |

One document per change-stream listener, same granularity as `projection_checkpoints` — kept as a
separate collection rather than folded into it, since a checkpoint (durable, advances only on
confirmed progress) and a lease (ephemeral, expires on its own) have different lifecycle semantics.

### 4.2 Relationships

```mermaid
erDiagram
  ACCOUNTS {
    string id PK
    string ownerId
    long initialBalance
    long currentBalance
    long version
  }
  TRANSACTIONS {
    string id PK
    string ownerId
    string accountId FK
    string invoiceId FK
    string type
    long amount
    array appliedProjections
  }
  BALANCE_SNAPSHOTS {
    string id PK
    string ownerId
    string accountId FK
    date asOfDate
    long balance
    string throughTransactionId
  }
  INVOICES {
    string id PK
    string ownerId
    long totalAmount
    long version
  }
  INVOICE_TOTAL_SNAPSHOTS {
    string id PK
    string ownerId
    string invoiceId FK
    date asOfDate
    long totalAmount
  }

  ACCOUNTS ||--o{ TRANSACTIONS : "has"
  INVOICES ||--o{ TRANSACTIONS : "has"
  ACCOUNTS ||--o{ BALANCE_SNAPSHOTS : "checkpointed by"
  INVOICES ||--o{ INVOICE_TOTAL_SNAPSHOTS : "checkpointed by"
```

`projection_checkpoints` is intentionally left out of this diagram — it's one row per *projector*,
not per account/invoice, so it doesn't participate in the tenant-data relationship graph above
(same reasoning `shedLock` is excluded from `005-cards`'s data model).

### 4.3 Migrations

No relational migrations (MongoDB, schema-first). New collections/fields introduced by this spec:

| Addition | Description |
|---|---|
| `accounts.currentBalance` | New field, backfilled once from the existing aggregation before the projector starts consuming |
| `invoices.totalAmount` | New field — no backfill needed, feature 005 has no data yet |
| `transactions.appliedProjections` | New field, array, defaults to `[]` on insert |
| `projection_checkpoints` collection | New, one document per projector |
| `projection_leases` collection | New, one document per change-stream listener (§4.1a) |
| `balance_snapshots`, `invoice_total_snapshots` collections | New |

---

## 5. API Contracts

No changes to any existing OpenAPI contract. `GET /accounts/{id}` and the future
`GET /invoices/{id}` response shapes are unchanged — only *how* `currentBalance`/`totalAmount` are
produced changes, not the response DTO. This closes the pre-existing gap where `AccountResponse`
was echoing `initialBalance` instead of a real computed balance (see §6.5 Rollout).

---

## 6. Cross-Cutting Concerns

### 6.1 Consistency

Eventual consistency between a transaction write and the projector applying it — typically
sub-second, not guaranteed. Documented explicitly (ADR-003 Consequences) rather than papered over;
acceptable for a personal-finance read model, not for a payment-authorization path.

### 6.2 Error Handling

- The change-stream listener fails mid-batch simply doesn't advance its checkpoint past the failed
  event; on restart it resumes from the last successfully-checkpointed token and republishes —
  safe because of the idempotency guard (§3.2).
- A `@SqsListener` consumer that throws leaves the message unacknowledged; SQS redelivers it after
  the visibility timeout, and after a bounded number of failed deliveries the redrive policy moves
  it to the DLQ — same failure-handling shape ADR-002 already established, reused as-is.
- The reconciliation job is the backstop for any projection bug that isn't a crash — a silent
  logic error that under- or over-increments is caught the next reconciliation cycle, not weeks
  later when a user notices a wrong balance.
- Self-heal writes are guarded by `_version`: if the reconciler's read-compare-write races a live
  projector `$inc`, the version mismatch fails the reconciler's write, which simply retries next
  cycle rather than clobbering a concurrent legitimate update.

### 6.3 Observability

| Signal | Name | When |
|---|---|---|
| Gauge | `projection.trigger.lag.seconds` | Age of the oldest unprocessed change-stream event, per change-stream listener |
| Gauge | `projection.queue.depth` | `mithril-vault-balance-projection` approximate message count (standard SQS queue metric, same as ADR-002's queue) |
| Counter | `projection.dlq.total` | Messages that exhausted the redrive policy and landed in the DLQ (tagged by consumer) |
| Counter | `projection.applied.total` | Every event successfully applied (tagged by consumer) |
| Counter | `projection.replay.noop.total` | Every replayed change-stream event or redelivered SQS message correctly no-op'd by the idempotency guard |
| Counter | `reconciliation.drift.total` | Every self-heal correction (tagged by entity type) — should be ~0 in steady state; a nonzero rate signals a projector bug |
| Counter | `reconciliation.version_conflict.total` | Self-heal writes that lost a race to `_version` (expected occasionally, concerning if frequent) |

A nonzero `reconciliation.drift.total` rate is the single most important signal this design
produces — it's the thing that turns "the balance is wrong" from a support ticket into an alert.

### 6.4 Security Considerations

No new public surface. `projection_checkpoints`, `balance_snapshots`, and
`invoice_total_snapshots` are read/written exclusively by infrastructure-layer background
processes, never exposed through a controller. Change-stream events carry `ownerId` on the
underlying transaction document already, so projectors preserve tenancy scoping (P2) by
construction — no cross-tenant read is possible since each `$inc` targets a document keyed by the
event's own `accountId`/`invoiceId`, already owned by that transaction's `ownerId`.

### 6.5 Rollout & Feature Flags

No flag — phased rollout order, since `003-accounts` has live data and `005-cards` does not:

1. Add `transactions.appliedProjections` field (defaults to `[]`, no behavior change).
2. Provision the `mithril-vault-balance-projection` SQS queue + DLQ (LocalStack seed script,
   alongside the existing `02-seed-sqs.sh` from ADR-002).
3. Add `accounts.currentBalance` field; run the one-time backfill script (seeds every existing
   account from the current aggregation).
4. Deploy `AccountBalanceChangeStreamListener` + `projection_checkpoints` (publishing only — no
   consumer live yet, so messages queue up harmlessly). Verify trigger lag before the next step.
5. Deploy `AccountBalanceProjector` (`@SqsListener`) + `BalanceSnapshotScheduler`. Verify the
   queued-up backlog from step 4 drains and `projection.applied.total` climbs correctly.
6. Deploy `BalanceReconciliationJob`; watch `reconciliation.drift.total` for a full cycle before
   trusting it as the safety net.
7. Flip `AccountController`/`AccountResponseMapper` to read `account.currentBalance()` instead of
   `account.initialBalance()` — this is also the fix for the pre-existing wiring gap where the
   read endpoints never surfaced a real computed balance.
8. Build `005-cards` (`InvoiceTotalProjector` etc.) with the pattern from day one — no backfill
   step needed, since there's no invoice data yet.

---

## 7. Requirements Traceability

`PRD-CROSS-01` (`docs/prd/PRD-CROSS-01-materialized-balance-projections.md`) was written
retroactively alongside this spec, scoped to the account-balance half only. Its FR/NFR IDs map
here; the architecture-level properties from ADR-003's Decision and Consequences sections that
don't have a direct PRD-level requirement (they're implementation properties the PRD requirements
depend on, not user-observable requirements themselves) are listed underneath for completeness.

| Requirement ID | Description (abbreviated) | Status | Where addressed |
|---|---|---|---|
| FR-001 | Fast, non-scanning balance read | ✅ Satisfied | §2.1, §3.2 — read path becomes a plain document fetch |
| FR-002 | Balance reflects new transactions without owner action | ✅ Satisfied | §3.1–§3.2 happy-path sequence |
| FR-003 | No transaction's balance update is ever lost | ✅ Satisfied | §3.1 — Change Stream observes every write regardless of code path |
| FR-004 | Direct-adjustment reconciliation stays immediate | ✅ Satisfied | ADR-003 "Reconciliation" — direct-adjustment path bypasses the async pipeline entirely |
| FR-005 | Auto-detect and self-heal balance drift | ⚠️ Deferred | §3.4 `BalanceReconciliationJob` — designed, not built this delivery |
| FR-006 | Pattern reusable for invoice totals | ✅ Satisfied | §3.1–§3.2 already parameterize both `AccountBalanceProjector`/`InvoiceTotalProjector` on one shared shape |
| NFR-001 | Read performance independent of history length | ✅ Satisfied | §2.1 — O(1) document fetch |
| NFR-002 | Sub-second, bounded update latency | ✅ Satisfied | §6.1 Consistency |
| NFR-003 | Idempotent under redelivery, no lost updates | ✅ Satisfied | §3.2 restart/replay diagram |
| NFR-004 | Auditability — traceable, immutable event log | ✅ Satisfied | §2.1 — `transactions` unchanged, only a new `appliedProjections` marker field |
| NFR-005 | Reuses existing SQS infra, not a second mechanism | ✅ Satisfied | §2.1 — SQS/`@SqsListener`/DLQ shape reused from ADR-002 |
| NFR-006 | Scale-agnostic design (P2 learning goal) | ✅ Satisfied | §6.4, ADR-003 Context |

**Implementation-level properties (ADR-003, not directly PRD-tied):**

| Property | Status | Where addressed |
|---|---|---|
| Bounded recomputation cost (snapshotting) | ⚠️ Deferred (with FR-005) | §3.3 `BalanceSnapshotScheduler` |
| Tenancy preserved (P2) | ✅ Satisfied | §6.4 |

---

## Appendix: Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Should `BalanceReconciliationJob`'s drift alert page someone, or is a metric + log line enough for a single-user app? | Architect | Open — no alerting infra exists yet in this project. |
| 2 | Should an admin/debug endpoint exist to trigger reconciliation on demand for one account (useful while building/testing this), or is the scheduled cadence enough? | Architect | Open. |
| 3 | Snapshot cadence — monthly, or every N transactions, or both (whichever comes first)? | Architect | Open — monthly assumed in ADR-003 and §3.3, not load-tested against any real cadence need. |
| 4 | Should `TransactionRepositoryAdapter.save` itself set `appliedProjections: []` explicitly, or rely on the projector's `findOneAndUpdate` filter treating a missing field as "not yet applied"? | Backend engineer | Open — an implementation detail to settle when this is actually built. |
| 5 | Should `AccountBalanceChangeStreamListener` and `AccountBalanceProjector` live in the same `@Component` (one class, listener method + `@SqsListener` method) or two separate classes? | Backend engineer | Open — an implementation detail; two classes probably reads clearer given they have genuinely different triggers/lifecycles, but not load-bearing either way. |
| 6 | Is cluster-wide single-active-listener safety needed, or is a single replica assumed for now? | Backend engineer | Resolved 2026-07-27 — a local k8s deployment with multiple pods is planned once this feature ships, so it's needed now rather than deferred. Addressed by `ProjectionLeaderElector` (§3.1a). |
