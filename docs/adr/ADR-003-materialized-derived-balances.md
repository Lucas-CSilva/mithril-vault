# ADR-003: Derived balances become event-driven materialized projections, not on-the-fly aggregations

> **Document metadata**
> - **ID:** ADR-003
> - **Status:** Proposed
> - **Author:** Architect
> - **Date:** 2026-07-26
> - **Supersedes:** P4 in `docs/architecture-contract.md` ("Derived values are computed, never
>   stored, and have one owner")
> - **Linked Specs:** `specs/003-accounts/data-model.md`, `specs/005-cards/data-model.md`,
>   `specs/007-dashboard/data-model.md`
> - **Related ADR:** `docs/adr/ADR-002-invoice-generation-via-sqs.md` (established the SQS/LocalStack
>   infra this ADR's projector transport reuses)
> - **Technical Solution:** `docs/technical-solutions/materialized-projections.md` (component
>   breakdown, sequence diagrams, package placement)

---

## Context

P4 of the architecture contract states that derived values — `Account.currentBalance`, `Invoice.
totalAmount`, `CreditCard.utilizedAmount`, budget `spentAmount`, dashboard "Saldo Líquido
Disponível" — are computed by read-side MongoDB aggregation and never persisted. This was a
reasonable simplification: it gives every derived value exactly one owner (the read side) and
avoids the classic bug class of a stored total drifting from its source rows.

It doesn't scale with transaction history. `AccountRepositoryAdapter.currentBalance` today runs:

```java
Aggregation aggregation =
    Aggregation.newAggregation(
        Aggregation.match(Criteria.where("ownerId").is(ownerId).and("accountId").is(accountId)),
        Aggregation.group("type").sum("amount").as("total"));
```

— an unbounded `$match` + `$group` + `$sum` over **every transaction the account has ever had**,
with no date bound. There is no caching, no snapshot, nothing that stops the scan from growing
linearly with account tenure. `specs/007-dashboard/data-model.md`'s "Saldo Líquido Disponível"
compounds this: it re-derives every account's balance *and* every open invoice's total in one
request. `specs/005-cards/data-model.md`'s `invoice.totalAmount` / `creditCard.utilizedAmount`
are specified the same way but not yet implemented (`Invoice`, `Card`, `InvoiceSummary`,
`CardSummary` are still stub records; `InvoiceRepositoryAdapter`/`CardRepositoryAdapter` return
`null`) — this is a genuine "design before building" opportunity, not a retrofit.

This project has exactly one real user, so none of this is an operational problem today. It is,
however, a deliberate learning goal (see P2's stated multi-user over-engineering, and ADR-002's
precedent of over-building invoice generation past what single-user scale requires) to understand
how real ledger and banking systems avoid re-scanning full history on every balance read. Research
into how production ledger systems solve this converges on three complementary techniques:

1. **Computed/materialized pattern** — store the derived total; update it incrementally instead
   of recomputing from scratch on every read (MongoDB's own "computed pattern"; standard CQRS
   read-model practice).
2. **Event-driven projection** — treat the append-only event log as the source of truth and apply
   it to a decoupled, asynchronously-updated read model, the same separation real ledger cores
   (Modern Treasury, Stripe-style) draw between the general ledger and the customer-facing account
   layer.
3. **Snapshotting** — periodic checkpoints bound how far back any recomputation (reconciliation,
   "balance as of date," audits) has to scan — the same technique bank statements and
   event-sourced systems use (opening/closing balance + deltas since the last checkpoint).

Sources consulted:
- [Ledger Pattern — AxonOps](https://axonops.com/docs/data-platforms/cassandra/application-development/patterns/ledger/)
- [Banking Database Design 2026 — Crassula](https://crassula.io/guides/banking-database-design/)
- [Exploring event sourcing: a scalable bank account — Medium](https://medium.com/@allousas/exploring-event-sourcing-a-scalable-bank-account-19b9d55302e0)
- [Accounting For Developers, Part II: Ledgering for a Wallet — Modern Treasury](https://www.moderntreasury.com/journal/accounting-for-developers-part-ii)
- [Ledger System Design — Fintechly](https://fintechly.com/infrastructure/infrastructure-ledger-system-design/)
- [MongoDB schema design patterns (computed pattern, bucket pattern) — dev.to](https://dev.to/mongodb/mongodb-schema-design-dos-and-donts-for-real-projects-1cg6)

---

## Decision

We introduce a single reusable **Projection** pattern and apply it first to account balance and
invoice/card totals. Future derived values (budget `spentAmount`, dashboard "Saldo Líquido") reuse
the same shape rather than re-deciding this.

### The Projection pattern

- **Source of truth stays the event log.** `transactions` already fits: append-only, immutable
  (P5 — corrections are new entries such as reversals, never in-place edits). No new event
  collection is introduced; the existing collection *is* the ledger.
- **Detection: a MongoDB Change Stream subscriber; delivery: SQS.** The API already runs against
  a Mongo replica set (a change stream on `transactions` gives an ordered, resumable event feed
  with no extra collection to maintain), and ADR-002 already introduced SQS/LocalStack/Spring
  Cloud AWS for invoice generation. Rather than choosing one or the other, this ADR splits the
  concern: the **Change Stream is the trigger** (it captures every write to `transactions`
  regardless of which code path produced it — a future transaction mode, an import path, a
  reversal — none of them can forget to fire the event, because none of them call anything to fire
  it; the change stream just sees the write), and **SQS is the transport** the trigger publishes
  to, reusing the exact `@SqsListener` + queue + DLQ shape ADR-002 already established rather than
  running two different asynchronous mechanisms side by side in this codebase:
  - `AccountBalanceChangeStreamListener` (infrastructure, subscribes to the `transactions` change
    stream) publishes a `BalanceProjectionMessage(ownerId, transactionId, accountId, invoiceId,
    type, amount, target)` to a new `mithril-vault-balance-projection` SQS queue on every
    insert/reversal — `target` distinguishes `ACCOUNT` vs `INVOICE` so one queue and one message
    shape serve both projections, same "one message shape for related triggers" instinct ADR-002
    used for `GenerateInvoiceMessage`.
  - `AccountBalanceProjector` (an `@SqsListener` consumer) receives the message and applies an
    atomic `$inc` on `accounts.currentBalance` for `target = ACCOUNT`.
  - `InvoiceTotalProjector` (a second `@SqsListener` on the same queue, or a `target`-based branch
    in one listener — an implementation detail, not an architectural one) applies the equivalent
    `$inc` on `invoices.totalAmount` for `target = INVOICE` (credit-card transactions only; not
    built until spec 005).
  - This was previously modeled as a straight "Change Stream applies `$inc` inline" design (see
    Alternatives, Option A-revised below) — moving the `$inc` behind SQS is what actually delivers
    the fan-out/decoupling/at-least-once-consumer properties this ADR wants to teach, the same
    lesson ADR-002 already drew for invoice generation, applied consistently instead of
    re-deciding it per feature.
- **Materialized field, same document, one owner.** `accounts.currentBalance` and
  `invoices.totalAmount` become real stored `Int64` fields on their respective collections. The
  read path becomes a single document fetch — no aggregation on the request path at all. "One
  owner" from the old P4 is preserved, just relocated: the *only* writer of these fields is the
  corresponding `@SqsListener` projector (plus the reconciliation job below, which is a controlled
  exception).
- **Checkpointing.** A new `projection_checkpoints` collection: `{projectionName, resumeToken,
  lastProcessedTransactionId, updatedAt}`, one document — scoped to the Change Stream listener,
  not the SQS consumers (SQS's own visibility-timeout/redelivery mechanics cover the
  listener-to-consumer leg; the checkpoint only needs to protect against the change-stream
  listener itself restarting and replaying from the beginning of the collection).
- **Idempotency guard.** Both legs of this pipeline are at-least-once: a restarted Change Stream
  listener can redeliver the same Mongo change event, and SQS can redeliver the same message. In
  either case `$inc` is not naturally idempotent, so it would double-count on replay. Each
  transaction records which projections have already applied it (e.g.
  `transactions.appliedProjections: ["accountBalance"]`, updated in the same operation as the
  `$inc` via a single `findOneAndUpdate` with a filter that excludes already-applied documents,
  run by the `@SqsListener` consumer — not the change-stream listener, since the consumer is the
  one actually writing the materialized field). A replayed event or redelivered message that finds
  itself already recorded is a no-op — one guard covers redelivery from either leg.
- **Snapshotting.** New `balance_snapshots` and `invoice_total_snapshots` collections:
  `{ownerId, accountId | invoiceId, asOfDate, balance, throughTransactionId}`, written on a
  schedule (e.g. monthly) or every N transactions. Any full recomputation — reconciliation,
  historical "balance as of date," audits — becomes "latest snapshot at or before the target date
  + transactions since," never a full-history scan.
- **Reconciliation job.** A scheduled job that, per account/invoice, recomputes ground truth from
  snapshot + deltas (this reuses the *existing* aggregation logic, now scoped by the snapshot date
  instead of full history) and compares it against the materialized field. On drift: log/alert,
  and self-heal by overwriting the materialized value — guarded by the existing `_version`
  optimistic-concurrency field (already used on `Invoice`, extended to `Account`) so the
  reconciler cannot race a concurrently-running projector update.
- **Read path change.** `AccountReadRepository.currentBalance` becomes a plain field read on
  `Account`. The existing aggregation is kept — renamed to something like `recomputeBalance` — and
  used only by the reconciliation job, the one-time backfill, and admin/debug tooling. It is never
  called from a request-serving read again.

### Applying the pattern to today's two cases

| | Account balance | Invoice total |
|---|---|---|
| Materialized field | `accounts.currentBalance` (new field) | `invoices.totalAmount` (new field) |
| Trigger (shared) | `AccountBalanceChangeStreamListener` (one listener, both targets) | same |
| Transport (shared) | `mithril-vault-balance-projection` SQS queue (one queue, both targets) | same |
| Consumer | `AccountBalanceProjector` (`@SqsListener`) | `InvoiceTotalProjector` (`@SqsListener`) |
| Snapshot collection | `balance_snapshots` | `invoice_total_snapshots` |
| Ground-truth aggregation | Existing `AccountRepositoryAdapter` pipeline, scoped from last snapshot | New pipeline (feature not yet built), scoped from last snapshot |
| Migration | One-time backfill script seeds `currentBalance` for every existing account before the projector goes live | None — feature 005 has no implementation yet, so it's built with this pattern from day one |

`specs/007-dashboard/data-model.md`'s "Saldo Líquido Disponível" is a **second-order projection**:
it composes the account-balance and invoice-total projections (`SUM(accounts.currentBalance) -
SUM(invoices.totalAmount WHERE status = OPEN)`) rather than re-deriving either from transactions.
Once the dashboard is built, this becomes a cheap in-memory sum over already-materialized fields
instead of the `$facet` double-aggregation currently sketched.

---

## Status

`Proposed` — 2026-07-26. Supersedes P4 in `docs/architecture-contract.md`; the contract's P4 text
is updated to summarize the new model and point here.

---

## Consequences

**Positive:**

- Balance/invoice-total reads are O(1) document fetches regardless of how much transaction history
  an account has accumulated — the core scalability problem this ADR exists to solve.
- The event log (`transactions`) is untouched and remains the auditable source of truth; nothing
  about P5 (immutability) changes.
- Self-healing: the reconciliation job catches drift from bugs or races and corrects it
  automatically, rather than requiring a human to notice a wrong balance.
- The pattern generalizes: budget `spentAmount` and the dashboard's "Saldo Líquido" adopt the same
  shape later without a fresh design decision.

**Negative:**

- Introduces an eventual-consistency window between a transaction being written and the projector
  applying it — now a two-hop window (Change Stream detects the write, publishes to SQS; the
  `@SqsListener` consumer applies the `$inc`) rather than one. For this app (personal finance, not
  a payment-authorization path) this is acceptable, but it must be stated explicitly: a
  `GET /accounts/{id}` immediately after `POST /transactions` can show a stale balance until both
  hops complete (typically sub-second, but not guaranteed).
- Meaningfully more moving parts than the pattern it replaces: a checkpoint collection, an
  idempotency guard, an SQS queue + DLQ, two new snapshot collections, and a reconciliation job —
  for an app with one real user. This is the deliberate over-engineering the user asked for, and
  future readers of this ADR should understand it as a stated learning exercise (see Context), not
  as a misjudgment of this project's actual scale.
- The materialized field and its projector must be kept behaviorally in sync with every kind of
  transaction mutation (create, reversal/correction) — a new transaction type that doesn't flow
  through the projector's event handling silently breaks the balance. This is the same "single
  point of truth must actually be single" discipline any materialized-view system requires.

**Failure modes worth documenting (the transferable lesson):**

- *Resume-token loss / change-stream listener restart replaying from an old checkpoint* →
  double-counting if the idempotency guard (`appliedProjections` marker) is ever skipped or
  removed as "unnecessary." This is why the guard is a required part of the design, not an
  optional hardening.
- *Out-of-order delivery* — SQS standard queues (the default, and what ADR-002 uses) do **not**
  guarantee order, so this is a live property of this design, not a hypothetical one. It's safe
  here because `$inc` is commutative: applying the same set of signed amounts to a balance
  produces the same result regardless of order. This guarantee would **not** hold if a future
  projection needed anything order-dependent (e.g. "last write wins" semantics, or a running
  balance-as-of-each-transaction materialized per row) — that's the case where a FIFO queue or a
  return to a strictly-ordered transport would become necessary. Documented here so a future
  reader doesn't assume this pattern generalizes to order-sensitive projections without
  re-checking this assumption.
- *Single-document write hotspot* — every transaction on one account serializes through one
  `$inc` on one document. At this project's actual scale this is irrelevant; it's the exact reason
  real high-throughput ledgers shard balances or avoid a single mutable balance document under
  heavy concurrent write load. Worth understanding, not worth solving here.

**Migration:**

A one-time backfill script runs the existing full-history aggregation once per account to seed
`accounts.currentBalance` before `AccountBalanceChangeStreamListener` starts publishing and
`AccountBalanceProjector` starts consuming. Invoices/cards need no backfill — feature 005 has no
data yet.

---

## Alternatives Considered

### Option A: Synchronous same-transaction `$inc` (rejected)

The `TransactionCommandHandler` that persists a `Transaction` also atomically `$inc`s
`Account.currentBalance` / `Invoice.totalAmount` in the same write path (optionally inside a
single Mongo multi-document transaction).

**Rejected because:** simpler and strongly consistent, but couples the transaction write path to
every derived value that must update, and teaches nothing about the event-driven/CQRS separation
that is the explicit point of this exercise. Kept as the natural fallback if the async model ever
proves more complexity than the learning value justifies.

### Option B: Manual outbox collection instead of MongoDB Change Streams (rejected — superseded, see below)

Write an `outbox` document in the same operation as the transaction; a poller drains it.

**Originally deferred because:** change streams give the same ordered, resumable event feed
without an extra collection, as long as the projector stays on the same Mongo replica set — the
outbox pattern was framed as worth adopting only once events need to leave Mongo.

**Superseded, not just deferred:** ADR-002 already brought SQS into this codebase for invoice
generation, so "events leaving Mongo" is no longer a hypothetical future trigger — it's already
true for a sibling feature. Per the Decision above, this ADR now routes through SQS too (Change
Stream as trigger, SQS as transport), which makes an explicit hand-rolled outbox collection
unnecessary either way: the Change Stream already gives the durability/completeness guarantee an
outbox exists to provide (nothing is missed because the trigger reads the oplog, not an
application-level publish call), so there was never a reason to add a *third* mechanism (outbox)
on top of the two already in play (Change Stream, SQS).

### Option A-revised: Change Stream applies `$inc` directly, no SQS hop (rejected)

A variant of the accepted Decision considered during this ADR's revision: keep the Change Stream
as both trigger *and* the thing that applies the `$inc`, with no SQS queue in between —
functionally close to the original Decision text before this revision.

**Rejected because:** it works, but it means this codebase runs two structurally identical
"async event → apply an effect, idempotently, with a DLQ for failures" mechanisms side by side —
Change-Stream-applies-directly for balances, SQS-consumer-applies for invoice generation — for no
reason other than which ADR happened to be written first. Routing the Change Stream's output
through the same SQS/`@SqsListener`/DLQ shape ADR-002 already established keeps exactly one
"how do async side effects get applied in this codebase" answer, and gets the queue's fan-out and
independent-consumer-scaling properties for free. The cost is one more hop (Change Stream →
publish → SQS → consume) versus applying inline inside the change-stream handler — accepted as
worth it for the consistency.

### Option C: Full event sourcing (no persisted entity state at all) (rejected)

Rebuild `Account`/`Invoice` state entirely from replaying events, with no persisted "current
state" document — the purist event-sourcing model.

**Rejected because:** too large a jump from the current hexagonal, aggregate-per-entity design.
`transactions` already functions as the append-only event log without requiring every entity in
the system to give up persisted state; this ADR captures the useful part of event sourcing
(events as source of truth, materialized projections as read models) without the full rewrite.

### Option D: Keep on-the-fly aggregation, bound only by a date range (rejected as insufficient alone)

Add a `since` filter to the existing aggregation (e.g. only sum the last 12 months) instead of
materializing anything.

**Rejected as insufficient because:** it caps cost but produces a *wrong* balance unless combined
with a snapshot to seed the starting point — which is exactly the snapshot component this ADR
already includes. Snapshotting alone (without materialization) would still leave every read paying
for an aggregation, just a smaller one; it doesn't get to O(1) reads, and doesn't teach the
projection/CQRS pattern that's the actual goal here.

---

## References

- `docs/architecture-contract.md` P4 (superseded by this ADR), P2 (multi-user over-engineering as
  a stated learning goal), P5 (immutability — transactions as append-only event log)
- `specs/003-accounts/data-model.md` — `currentBalance` derivation, updated to reference this ADR
- `specs/005-cards/data-model.md` — `invoice.totalAmount`, `creditCard.utilizedAmount`, updated to
  reference this ADR
- `specs/007-dashboard/data-model.md` — "Saldo Líquido Disponível" as a second-order projection
- `docs/adr/ADR-002-invoice-generation-via-sqs.md` — precedent for "reconciliation as the
  resilience mechanism," and the source of the SQS/LocalStack/`@SqsListener`/DLQ infrastructure
  this ADR's projector transport reuses directly (queue naming, message-shape convention,
  idempotency-via-unique-constraint-or-marker pattern)
- `api/src/main/java/com/mithrilvault/api/infrastructure/adapter/persistence/AccountRepositoryAdapter.java`
  — the unbounded aggregation this ADR replaces as the request-serving read path
