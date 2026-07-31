# Implementation Notes — Change-Stream Trigger (`AccountBalanceChangeStreamListener`)

This file supplements `materialized-projections.md` (SPEC-CROSS-01) with the *how* behind just the
**trigger/publish side** of that design — the piece currently scratched in
`AccountBalanceChangeStreamListener`. It does not cover `AccountBalanceProjector`,
`BalanceSnapshotScheduler`, or `BalanceReconciliationJob` (SPEC-CROSS-01 §3.2–§3.4) — those are
separate, later pieces of the rollout (§6.5 steps 5–6) and out of scope here.

Stack versions for the doc links: **Spring Boot 4.0.6** (Spring Data MongoDB 5.x, Spring Cloud AWS
4.0.0-RC1 — see `api/gradle/libs.versions.toml`).

---

## 1. What's broken in the current scratch code

`infrastructure/adapter/projection/AccountBalanceChangeStreamListener.java` today:

```java
private Disposable a() {
  return reactiveMongoTemplate.changeStream(TransactionDocument.class)
      .watchCollection("transactions")
      .filter(where("operationType").is("insert"))
      .resumeAt(projectionCheckpointRepository.findById("accountBalance")
          .map(ProjectionCheckpointDocument::getResumeToken))
      .listen()
      .flatMap(event -> {
        var transaction = event.getBody();
        var message = new TransactionDocument();
        return Mono.fromFuture(sqsTemplate.sendAsync("mithril-vault-balance-projection", message))
            .then(projectionCheckpointRepository.save(new ProjectionCheckpointDocument()));
      })
      .subscribe();
}
```

Five concrete defects, in the order you'll hit them while rewriting:

1. **`a()` is never called.** Private method, no lifecycle hook (`@PostConstruct`,
   `ApplicationReadyEvent`, `SmartLifecycle`) invokes it. Nothing runs at boot.
2. **`resumeAt(Mono<...>)` doesn't compile against the real API.** `resumeAt`/`resumeAfter` on
   `ReactiveChangeStreamOperation$ChangeStreamWithFilterSpec` take a synchronous `Instant` or
   `BsonValue`/resume-token document — not a `Mono`. You must resolve the checkpoint *before*
   entering the builder chain.
3. **`.filter(where("operationType").is("insert"))` is actually correct** — don't "fix" this. It's
   `ReactiveChangeStreamOperation`'s own fluent builder method (compiles to an aggregation
   `$match` stage passed to the underlying MongoDB Change Stream, not `reactor.core.publisher.Flux#filter`).
   Confirmed against the Spring Data MongoDB docs (see §5 References).
4. **The published message is thrown away.** `var transaction = event.getBody();` is read and then
   never used — `new TransactionDocument()` (all-null) is sent to SQS instead.
5. **The checkpoint write is a no-op/garbage write.** `new ProjectionCheckpointDocument()` has no
   `@Id` set and no `resumeToken`/`lastProcessedTransactionId` populated — it neither advances the
   `"accountBalance"` row nor upserts correctly.

---

## 2. Where this lives (hexagonal layering)

```
infrastructure/adapter/messaging/BalanceProjectionMessage.java     — record, SQS payload shape
infrastructure/adapter/messaging/ProjectionTarget.java             — enum {ACCOUNT, INVOICE}
infrastructure/adapter/messaging/BalanceProjectionQueuePublisher.java — wraps SqsTemplate
infrastructure/adapter/projection/BalanceProjectionMessageFactory.java — TransactionDocument → message
infrastructure/adapter/projection/ProjectionCheckpointStore.java   — read/advance the checkpoint row
infrastructure/adapter/projection/ProjectionLeaderElector.java     — multi-instance lease/failover
infrastructure/adapter/projection/AccountBalanceChangeStreamListener.java — orchestrator (rewritten)
infrastructure/persistence/document/ProjectionLeaseDocument.java   — new, backs the leader elector
infrastructure/persistence/document/ProjectionCheckpointDocument.java — existing, rename lastModified→updatedAt
```

**Why `messaging` is a separate package from `projection`:** per
`docs/architecture-contract.md:224-236`, `infrastructure → domain (+ Spring Data, Mongo)` is the
only sanctioned dependency shape for this layer — `application/` is reserved for the REST-facing
tier and is explicitly forbidden from depending on `infrastructure`. All of the classes above sit
in `infrastructure`, so that constraint isn't in play here; the `messaging`/`projection` split
instead follows `materialized-projections.md:305-317` (§3.5 package-placement table), which leaves
the publish call's exact home as an open call for whoever builds it (§3.5 row 3) — resolved here as
a small dedicated `messaging` package, matching the doc's suggested "small
`infrastructure/adapter/messaging` gateway" option, and set up so ADR-002's own invoice-generation
publish call can move there too for parity later (its `SqsTemplate` usage isn't in the codebase yet
— confirmed via search, `AccountBalanceChangeStreamListener` is currently the only SQS-related file
in `api/src/main/java`).

---

## 3. Component by component

### 3.1 `BalanceProjectionMessage` + `ProjectionTarget`

Reference: `materialized-projections.md:156-159` (§3.1) — exact field list already specified:

```java
public record BalanceProjectionMessage(
    String ownerId,
    String transactionId,
    String accountId,
    String invoiceId,
    String type,
    Long amount,
    ProjectionTarget target) {}

public enum ProjectionTarget { ACCOUNT, INVOICE }
```

No behavior — pure transport DTO. Never reuse `TransactionDocument` here (that was the scratch
code's bug #4) — the whole point of a dedicated message type is that the SQS contract doesn't leak
persistence-layer shape changes.

### 3.2 `BalanceProjectionMessageFactory`

Pure mapping, no I/O, no Spring annotations needed beyond `@Component` for DI convenience:

```java
public BalanceProjectionMessage from(TransactionDocument transaction) {
  ProjectionTarget target = transaction.getInvoiceId() != null
      ? ProjectionTarget.INVOICE
      : ProjectionTarget.ACCOUNT;
  return new BalanceProjectionMessage(
      transaction.getOwnerId(),
      transaction.getId(),
      transaction.getAccountId(),
      transaction.getInvoiceId(),
      transaction.getType(),
      transaction.getAmount(),
      target);
}
```

This mirrors the existing account/invoice XOR convention already established for `Transaction`
(`specs/004-transactions/implementation-notes.md:56-67` — exactly one of `accountId`/`invoiceId` is
ever set), so branching on "is `invoiceId` present" is safe and matches an invariant this codebase
already enforces at write time.

### 3.3 `BalanceProjectionQueuePublisher`

```java
private static final String QUEUE_NAME = "mithril-vault-balance-projection";

public Mono<Void> publish(BalanceProjectionMessage message) {
  return Mono.fromFuture(sqsTemplate.sendAsync(QUEUE_NAME, message)).then();
}
```

Isolates `SqsTemplate` (an `io.awspring.cloud.sqs` type) behind one method so
`AccountBalanceChangeStreamListener` never imports it directly. This is the one piece not covered
by the design doc's diagrams in detail — it exists purely to satisfy the "don't put everything in
the listener" requirement.

### 3.4 `ProjectionCheckpointStore`

Wraps `ProjectionCheckpointMongoRepository` (already exists, untouched) with two intention-revealing
methods:

```java
public Mono<BsonValue> findResumeToken(String projectionName) {
  return checkpointRepository.findById(projectionName)
      .map(ProjectionCheckpointDocument::getResumeToken)
      .map(Document::toBsonDocument); // or however the stored type converts back to a resume token
}

public Mono<Void> advance(String projectionName, BsonValue resumeToken, String lastProcessedTransactionId) {
  return checkpointRepository.findById(projectionName)
      .defaultIfEmpty(ProjectionCheckpointDocument.builder().projectionName(projectionName).build())
      .flatMap(cp -> {
        cp.setResumeToken(resumeToken); // adapt type to whatever ProjectionCheckpointDocument.resumeToken actually is
        cp.setLastProcessedTransactionId(lastProcessedTransactionId);
        return checkpointRepository.save(cp);
      })
      .then();
}
```

This is scratch code's bug #5, fixed: `advance` always operates on the row keyed by
`projectionName`, never constructs a detached empty document. While touching
`ProjectionCheckpointDocument`, rename `lastModified` → `updatedAt` to match the schema in
`materialized-projections.md:328-336` (§4.1) — confirmed with you, nothing else reads this field
yet.

### 3.5 `ProjectionLeaderElector` — the multi-instance answer

This is new relative to the design doc — SPEC-CROSS-01 doesn't mention multi-instance safety for
the *listener* at all (it does ShedLock-guard the schedulers in §3.3/§3.4, but says nothing about
the change-stream listener itself racing across replicas). Worth eventually folding back into that
doc; flagged here so it isn't lost.

**Why not ShedLock** (which this codebase already references for the schedulers, per
`materialized-projections.md:257,268`): ShedLock's model is "acquire a lock for a bounded
`lockAtMostFor` duration around one execution of a `@Scheduled` method," then release. A change
stream listener isn't a discrete scheduled execution — it's a subscription meant to be held
*indefinitely*, with automatic hand-off only on crash. Forcing that into ShedLock's bounded-lock
model would mean either re-acquiring constantly (defeats the "long-lived subscription" point) or
setting an artificially huge `lockAtMostFor` (delays failover by that same huge duration if the
leader dies). A heartbeat-lease pattern — renew a short-TTL lock continuously while alive — fits a
long-running subscription instead of a discrete job. This is a standard reactive leader-election
pattern (see §5 References for background); implementing it directly against MongoDB keeps this
consistent with `projection_checkpoints` (already a single-document-per-key Mongo table) rather
than introducing a new locking library/dependency for one component.

**New document** `ProjectionLeaseDocument` (collection `projection_leases`):

```java
@Document(collection = "projection_leases")
public class ProjectionLeaseDocument {
  @Id private String projectionName;   // e.g. "accountBalance"
  private String instanceId;
  private Instant leaseExpiresAt;
}
```

**Acquire-or-renew**, atomic via `findAndModify` so two instances racing can't both "win":

```java
public Mono<Boolean> tryAcquireOrRenew(String projectionName, String instanceId, Duration leaseTtl) {
  Query query = query(where("projectionName").is(projectionName)
      .orOperator(
          where("instanceId").is(instanceId),
          where("leaseExpiresAt").lt(Instant.now())));
  Update update = new Update()
      .set("instanceId", instanceId)
      .set("leaseExpiresAt", Instant.now().plus(leaseTtl))
      .setOnInsert("projectionName", projectionName);
  FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);
  return reactiveMongoTemplate.findAndModify(query, update, options, ProjectionLeaseDocument.class)
      .map(lease -> lease.getInstanceId().equals(instanceId))
      .defaultIfEmpty(false);
}
```

Note: a plain `upsert(true)` with an `$or` filter races a fresh document's default field values on
first-ever insert — worth a quick correctness pass together once you've written the real version
(`setOnInsert` vs. `set` ordering matters for the very first acquire when no document exists yet).
This is exactly the kind of subtlety worth pairing on rather than me writing it outright.

**Leadership signal + gating the stream:**

```java
public Flux<Boolean> leadershipSignal(String projectionName, String instanceId, Duration leaseTtl) {
  return Flux.interval(leaseTtl.dividedBy(3))
      .flatMap(tick -> tryAcquireOrRenew(projectionName, instanceId, leaseTtl))
      .distinctUntilChanged()
      .startWith(false);
}
```

```java
leaderElector.leadershipSignal("accountBalance", instanceId, LEASE_TTL)
    .switchMap(isLeader -> isLeader ? buildChangeStreamFlux() : Flux.empty())
```

`switchMap` is what gives you the failover behavior for free: losing leadership cancels the inner
`buildChangeStreamFlux()` subscription automatically (Reactor's contract for `switchMap` —
switching to a new inner publisher disposes the previous one), so there's no manual
dispose/reconnect logic to write for the handoff case itself. Suggested `LEASE_TTL = Duration.ofSeconds(30)`,
renew interval `10s` — tunable, not load-bearing for correctness.

**`instanceId`** — generate once at startup, no external dependency needed:
```java
private final String instanceId = InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
```

### 3.6 `AccountBalanceChangeStreamListener` (rewritten orchestrator)

Lifecycle: implement `SmartLifecycle` rather than `@PostConstruct` — gives you an explicit
`stop()` to `dispose()` the subscription on shutdown, which `@PostConstruct` doesn't:

```java
@Component
@RequiredArgsConstructor
public class AccountBalanceChangeStreamListener implements SmartLifecycle {

  private static final String PROJECTION_NAME = "accountBalance";
  private Disposable subscription;

  @Override
  public void start() {
    subscription = leaderElector.leadershipSignal(PROJECTION_NAME, instanceId, LEASE_TTL)
        .switchMap(isLeader -> isLeader ? changeStreamFlux() : Flux.empty())
        .subscribe();
  }

  @Override
  public void stop() {
    if (subscription != null) subscription.dispose();
  }

  @Override
  public boolean isRunning() {
    return subscription != null && !subscription.isDisposed();
  }

  private Flux<ChangeStreamEvent<TransactionDocument>> changeStreamFlux() {
    return Flux.defer(() -> checkpointStore.findResumeToken(PROJECTION_NAME))
        .flatMap(token -> buildStream(builder -> builder.resumeAfter(token)))
        .switchIfEmpty(Flux.defer(() -> buildStream(builder -> builder))) // no checkpoint yet — start from now
        .concatMap(this::handleEvent); // concatMap, not flatMap — must stay strictly ordered
  }

  private Mono<Void> handleEvent(ChangeStreamEvent<TransactionDocument> event) {
    return Mono.just(event.getBody())
        .map(messageFactory::from)
        .flatMap(queuePublisher::publish)
        .then(checkpointStore.advance(PROJECTION_NAME, event.getResumeToken(), event.getBody().getId()))
        .onErrorResume(e -> {
          log.error("Failed to process balance-projection event, checkpoint not advanced", e);
          return Mono.empty(); // don't propagate — next restart resumes from last good checkpoint
        });
  }
}
```

Key ordering rule (this is the direct answer to your original race-condition question):
**publish-then-checkpoint, never the reverse** — `handleEvent` only calls `checkpointStore.advance`
*after* `queuePublisher.publish` completes successfully via `.then(...)`, and any failure in that
chain is swallowed by `onErrorResume` *before* the checkpoint write, not after. `concatMap` (not
`flatMap`) keeps events strictly sequential so the checkpoint only ever advances in event order —
a `flatMap` here would let events complete out of order and risk checkpointing past an event whose
publish actually failed.

`.filter(where("operationType").is("insert"))` from the original scratch code is preserved
unchanged inside `buildStream` — it's correct as-is (see §1 point 3).

---

## 4. Files to create / change

| File | Action |
|---|---|
| `infrastructure/adapter/messaging/BalanceProjectionMessage.java` | new |
| `infrastructure/adapter/messaging/ProjectionTarget.java` | new |
| `infrastructure/adapter/messaging/BalanceProjectionQueuePublisher.java` | new |
| `infrastructure/adapter/projection/BalanceProjectionMessageFactory.java` | new |
| `infrastructure/adapter/projection/ProjectionCheckpointStore.java` | new |
| `infrastructure/adapter/projection/ProjectionLeaderElector.java` | new |
| `infrastructure/persistence/document/ProjectionLeaseDocument.java` | new |
| `infrastructure/adapter/projection/AccountBalanceChangeStreamListener.java` | rewrite |
| `infrastructure/persistence/document/ProjectionCheckpointDocument.java` | rename `lastModified`→`updatedAt` |
| `ProjectionCheckpointMongoRepository.java`, `BalanceSnapshotDocument.java` | untouched |

---

## 5. References

- **This codebase:**
  - `docs/technical-solutions/materialized-projections.md` §2.1, §3.1, §3.5, §4.1, §6.2, §6.5,
    Appendix Q5 — the approved design this implements.
  - `docs/architecture-contract.md:220-236` — hexagonal package/dependency rules.
  - `specs/004-transactions/implementation-notes.md:56-67` — existing account/invoice XOR
    convention, reused for `ProjectionTarget` branching.
- **Spring Data MongoDB (reactive change streams):**
  - [Change Streams — Spring Data MongoDB reference](https://docs.spring.io/spring-data/mongodb/reference/mongodb/change-streams.html)
    — `resumeAt(Instant)` vs. token-based resume; confirms these are synchronous builder args, not
    reactive types.
  - [`ReactiveChangeStreamOperation` Javadoc](https://docs.spring.io/spring-data/mongodb/docs/current/api/org/springframework/data/mongodb/core/ReactiveChangeStreamOperation.html)
    — confirms `.filter(Criteria)` compiles to an aggregation `$match`, not `Flux#filter`.
  - [MongoDB Change Streams with Reactive Spring Data — Vinsguru](https://www.vinsguru.com/mongodb-change-streams-reactive-spring-data/)
    — worked example of the `resumeAfter(token)` vs. `resumeAt(Instant)` fallback branch used in
    §3.6 above.
- **Leader election background** (informed the heartbeat-lease design in §3.5; no single canonical
  Java/Mongo library fit a long-running reactive subscription, so this is a small custom pattern
  rather than an off-the-shelf dependency):
  - [ShedLock](https://github.com/lukas-krecan/ShedLock) — confirms its model is bounded
    `lockAtMostFor` per scheduled execution, why it doesn't fit here directly.
  - [Spring Cloud Kubernetes — Leader Election](https://docs.spring.io/spring-cloud-kubernetes/reference/leader-election.html)
    — general lease/TTL leader-election shape this design's `ProjectionLeaseDocument` mirrors,
    adapted to plain MongoDB instead of the Kubernetes Lease API (no k8s dependency in this
    project).

---

## Summary checklist

- [ ] `BalanceProjectionMessage` + `ProjectionTarget` (`infrastructure/adapter/messaging`)
- [ ] `BalanceProjectionQueuePublisher` wrapping `SqsTemplate`
- [ ] `BalanceProjectionMessageFactory` — pure `TransactionDocument → BalanceProjectionMessage`
- [ ] `ProjectionCheckpointStore` — `findResumeToken` / `advance`, upsert-by-`projectionName`
- [ ] Rename `ProjectionCheckpointDocument.lastModified` → `updatedAt`
- [ ] `ProjectionLeaseDocument` + `ProjectionLeaderElector` — atomic `findAndModify` acquire/renew,
      `leadershipSignal()` Flux
- [ ] `AccountBalanceChangeStreamListener` rewritten as `SmartLifecycle`, gated by
      `leadershipSignal().switchMap(...)`, `concatMap` for strict ordering, publish-then-checkpoint,
      errors swallowed before the checkpoint write
- [ ] Unit tests: `BalanceProjectionMessageFactory` (pure), `ProjectionCheckpointStore.advance`
      (first-write vs. existing-row), `ProjectionLeaderElector` (two-instance race + expired-lease
      takeover)
- [ ] Integration test: insert a transaction, assert a message lands on the LocalStack
      `mithril-vault-balance-projection` queue with correct shape, assert checkpoint advanced
- [ ] Manual check: `docker compose up -d mongodb localstack` + `./gradlew bootRun`, insert a
      transaction, `awslocal sqs receive-message` against the queue
