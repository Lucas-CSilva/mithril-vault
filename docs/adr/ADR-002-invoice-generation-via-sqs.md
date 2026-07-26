# ADR-002: Invoice generation is event-driven via SQS, with the monthly job as a self-healing reconciler

> **Document metadata**
> - **ID:** ADR-002
> - **Status:** Accepted
> - **Author:** Architect
> - **Date:** 2026-07-26
> - **Linked PRD:** PRD-005 — Cards & Invoices (FR-002, FR-003, FR-004, NFR-006, NFR-007, NFR-008)
> - **Linked Spec:** `specs/005-cards/technical-solution.md`

---

## Context

PRD-005 requires that every credit card always has an OPEN invoice for the current and next
reference month (FR-002, FR-003), that generation never double-creates an invoice for the same
card/month (FR-004), and — deliberately, as a learning exercise beyond what this single-user
instance actually needs — that the mechanism holds up if the card count were millions rather than
a handful (NFR-006, NFR-007, NFR-008).

The straightforward implementation is `docs/implementation-plan.md`'s original sketch: an
`InvoiceGenerationService` called synchronously inline from `CreateCardCommandHandler`, and a
single `@Scheduled` method that loops over every active card once a month and creates missing
invoices in the same request. That works today. It does not hold up under the explicit scale
goal: a synchronous loop over "every active card" blocks the scheduler thread for as long as the
full scan takes, a `collectList()`-style read of every active card holds the entire result set in
memory, one card's failure (a stale `associatedAccountId`, a transient Mongo hiccup) can abort the
whole batch depending on how the loop is written, and if two instances of the API run
concurrently (any horizontally-scaled deployment), both fire the same `@Scheduled` method and race
to create the same invoices.

The project already runs LocalStack (`docker-compose.yml`, currently `SERVICES=secretsmanager`)
and already depends on Spring Cloud AWS (`spring-cloud-aws-starter-secrets-manager`,
`libs.versions.toml`) for the same LocalStack-endpoint-override pattern used elsewhere
(`api/src/main/resources/application-local.yaml`). Introducing SQS reuses that infra rather than
standing up a new broker (RabbitMQ/Kafka) purely for this exercise.

---

## Decision

We will make invoice generation **event-driven via SQS (through LocalStack)**, with a single
message shape reused by both triggers, and the monthly job re-derived as a **reconciler** rather
than a one-shot batch — so a lost message is never a permanent loss, only a delay until the next
cycle.

Concretely:

- **One message shape:** `GenerateInvoiceMessage(ownerId, cardId, referenceMonth)`, published to a
  `mithril-vault-invoice-generation` SQS queue.
- **Card creation (FR-002):** `CreateCardCommandHandler` saves the card, then publishes two
  messages (current month, next month) instead of calling `InvoiceGenerationService` inline. The
  `POST /cards` response returns as soon as the card is saved — it does not wait on invoice
  creation.
- **Monthly rollover (FR-003):** a `@Scheduled` trigger, guarded by a distributed lock (ShedLock,
  backed by Mongo — the only shared store already in place) so exactly one instance fires per
  cycle, streams active cards via `CardReadRepository` as a `Flux` (no `collectList`, bounded
  concurrency on publish) and publishes one `GenerateInvoiceMessage` per active card for the
  upcoming reference month.
- **One consumer:** a single `@SqsListener` receives both kinds of messages (card-creation
  bootstrap and monthly rollover are indistinguishable to the consumer — both are just "ensure
  this card has an invoice for this reference month") and performs a find-or-create against the
  existing unique index `{ownerId, creditCardId, referenceMonth}` (`data-model.md`).
- **Idempotency (FR-004, NFR-008):** the unique index is the actual idempotency guarantee, not the
  queue. A redelivered or duplicated message hits `DuplicateKeyException` on the second attempt,
  handled as a silent no-op — the same pattern already established for CSV/OFX dedup in
  `004-transactions`. This means the consumer needs no separate idempotency-key tracking table.
- **No transactional outbox.** Given the reconciliation property below, we explicitly do **not**
  build a transactional outbox for the card-creation publish step. This is a deliberate, stated
  tradeoff (see Alternatives), not an oversight.
- **Reconciliation as the resilience mechanism (NFR-007):** the monthly job does not remember what
  it published last cycle — every run re-derives "active cards missing an invoice for the target
  month" from the database and republishes only for those. If a card-creation message is lost (API
  crash between saving the card and publishing, or a dropped SQS delivery), the very next monthly
  cycle notices the card has no invoice for the current/next month and generates it. Worst case:
  one month's delay, never a permanent gap.
- **Failure handling:** the SQS queue has a redrive policy to a dead-letter queue after a bounded
  number of failed deliveries; DLQ messages are logged with `cardId`/`referenceMonth` for manual
  replay, and — because of the reconciler property above — are a monitoring/latency concern, not a
  data-loss risk.

---

## Status

`Accepted` — 2026-07-26

---

## Consequences

**Positive:**

- Card creation (`POST /cards`) response latency no longer includes invoice-generation work.
- The monthly job scales by fan-out instead of by a single thread's loop duration — one message
  per card, processed independently, in parallel, by however many consumer instances exist.
- No coordinated multi-instance duplicate-work problem for the *consumer* side: SQS's
  at-least-once delivery plus the unique-index idempotency check makes concurrent consumers safe
  by construction.
- The reconciliation property turns "a message got lost" from an incident into a footnote — this
  is the same shape as any real reconciliation job (bank statement reconciliation, billing
  re-runs) and is the most transferable lesson from this exercise.

**Negative:**

- Adds real operational surface area for a single-user app: a new LocalStack service (`sqs`), a
  new Gradle dependency (`spring-cloud-aws-starter-sqs`), a queue + DLQ to provision
  (`localstack/init/02-seed-sqs.sh`), and a distributed-lock dependency (ShedLock) purely to
  prevent a problem (two API instances) that does not exist at this project's actual scale.
- Introduces an eventual-consistency window: a card can exist for a short time before its invoices
  do. `TransactionOriginResolver.findOpenInvoice` (currently 404s immediately, see the
  `TODO(005-cards)` at `TransactionOriginResolver.java:49`) must tolerate this window rather than
  assuming the invoice is present synchronously after card creation — this is a real design
  consequence for `004-transactions`, not just for this feature.
- One more moving part to operate and monitor (queue depth, DLQ, ShedLock table) than a plain
  `@Scheduled` loop would have needed.

**Risks and mitigations:**

- *Risk:* the eventual-consistency window described above causes `findOpenInvoice` to 404 on a
  transaction posted immediately after card creation, before the async consumer has run.
  *Mitigation:* out of this ADR's scope to resolve fully, but flagged explicitly for
  `technical-solution.md` and the eventual `TransactionOriginResolver` fix — options include a
  short synchronous retry/backoff in the resolver, or making card creation wait for the first
  `GenerateInvoiceMessage` round-trip before returning 201 (which would partially undercut the
  latency benefit above and needs its own decision when that code is written).
- *Risk:* ShedLock's Mongo collection is one more piece of shared infrastructure state to reason
  about. *Mitigation:* it holds no user data (no `ownerId`), is purely a lock row keyed by job
  name, and its failure mode (lock not released) is bounded by a lock TTL, not indefinite.

---

## Alternatives Considered

### Option A: Synchronous `InvoiceGenerationService`, called inline everywhere (rejected — was the original plan)

`docs/implementation-plan.md`'s original sketch: card creation calls the service directly; the
monthly `@Scheduled` method loops over every active card and calls the same service in-process.

**Rejected because:**

- Does not meet NFR-006/NFR-007 as stated — a single-threaded loop over "every active card" scales
  linearly with card count with no fan-out, and one card's failure can abort the batch depending on
  loop structure.
- Multiple API instances would each run the same `@Scheduled` method with no coordination,
  racing to create the same invoices (mitigated only by the unique index catching the race after
  the fact, not preventing duplicate work).
- This is precisely the scale problem the user explicitly wants to exercise; keeping the naive
  version would defeat the stated purpose of this design pass.

### Option B: Mongo-based outbox (deferred)

An `outbox` collection written in the same transaction as the card save; a poller or Mongo change
stream drains it and performs invoice creation.

**Deferred because:**

- Genuinely a stronger guarantee for the card-creation path (true atomicity between "card exists"
  and "bootstrap event recorded"), and arguably more realistic for a real fintech backend than a
  general-purpose queue.
- Rejected only in favor of SQS for this exercise because the reconciliation property (monthly
  job re-derives missing invoices from scratch) already closes the same gap the outbox would close
  for this specific feature, at lower implementation cost — the outbox pattern is worth revisiting
  if a future feature needs an atomicity guarantee that a periodic reconciler cannot provide (e.g.
  a case where "eventually consistent, worst case one month late" is not acceptable).

### Option C: RabbitMQ or Kafka (rejected)

Stand up a dedicated broker in `docker-compose.yml` for this feature.

**Rejected because:**

- LocalStack is already running for Secrets Manager, and Spring Cloud AWS is already a dependency
  — SQS reuses both with no new service to operate locally, and keeps the project's infra choices
  consistent (AWS-flavored) rather than introducing a second, unrelated messaging paradigm.
- Kafka in particular is disproportionate to this project's actual throughput and would add
  partition/consumer-group concepts with no corresponding benefit here.

### Option D: Full transactional outbox + change-data-capture pipeline (deferred)

A CDC-based pipeline (e.g. Debezium reading the Mongo oplog) publishing to SQS or Kafka instead of
an application-level publish call.

**Deferred because:**

- Meaningfully more infrastructure (CDC connector, its own failure modes) than this feature's
  actual requirements justify, even under the "over-engineer as an exercise" framing. Revisit only
  if a future feature needs guaranteed exactly-once delivery semantics that SQS + idempotent
  consumers cannot provide.

---

## References

- PRD-005 §5 (FR-002, FR-003, FR-004), §6 (NFR-006, NFR-007, NFR-008)
- `specs/005-cards/data-model.md` — unique index `{ownerId, creditCardId, referenceMonth}`,
  Invoice Lifecycle section
- `specs/005-cards/technical-solution.md` — component breakdown, sequence diagrams for both
  publish paths
- `docs/implementation-plan.md:187` — original (superseded) synchronous sketch
- `api/src/main/java/com/mithrilvault/api/domain/service/TransactionOriginResolver.java:49` —
  the `TODO(005-cards)` this decision directly affects
- `docker-compose.yml`, `localstack/init/01-seed-secrets.sh` — existing LocalStack pattern this
  ADR extends
