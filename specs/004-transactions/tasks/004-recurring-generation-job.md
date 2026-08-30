# Task 004 — RecurringTransactionGenerationJob

## Scope

**In scope:** a new `@Scheduled` job that generates each recurring series' next instance as its
date comes due, and advances the series. Same shape as
`infrastructure/scheduler/BalanceReconciliationJob.java` / `BalanceSnapshotJob.java`: a
`@DistributedLock`-guarded method, streamed (not `collectList()`'d) query, per-item error
isolation (one series' failure doesn't abort the run for the rest).

**Out of scope:** anything about the balance projection itself — each generated instance is a
plain `Transaction` insert; `AccountBalanceChangeStreamListener` picks it up exactly like any
other insert, no special-casing needed here.

## Depends on

`tasks/001-shared-port-extensions.md` (`RecurringSeriesReadRepository.findDueSeries`,
`RecurringSeriesRepository.advance`), `tasks/003-recurring-series-and-instance.md`
(`RecurringTransactionSeries` shape).

## Files touched

- `infrastructure/scheduler/RecurringTransactionGenerationJob.java` — new
- `domain/config/AppProperties.java` — add a `SchedulerJobConfig recurringGeneration` entry to
  `SchedulerConfig`, same pattern as the existing `balanceSnapshot`/`balanceReconciliation` entries
- `src/main/resources/application*.yaml` — add the job's cron entry under `app.scheduler`
- `infrastructure/scheduler/RecurringTransactionGenerationJobTest.java` — new
- `*IT` — one integration test exercising a due series end-to-end

## Acceptance Criteria

- **AC-T04-1:** A series with `nextOccurrenceDate == today` generates exactly one new
  `Transaction` (using the series' template fields) and advances `nextOccurrenceDate` by
  `frequency`.
- **AC-T04-2:** A series with `nextOccurrenceDate` in the future generates nothing when the job
  runs.
- **AC-T04-3:** A series whose *next* `nextOccurrenceDate` (after advancing) would exceed
  `endDate` still generates the due instance, but the series is not picked up again on a
  subsequent run once `nextOccurrenceDate > endDate`.
- **AC-T04-4:** One series throwing during generation (e.g. a transient Mongo error) does not
  prevent other due series in the same run from being processed — mirror
  `BalanceReconciliationJob`'s per-item error isolation, not a single `Flux` that aborts on first
  error.
- **AC-T04-5:** `@DistributedLock` prevents two concurrent instances of the job from double-
  generating the same series' instance (same guarantee the existing jobs already have — this is a
  reuse, not a new mechanism to independently verify beyond confirming the annotation is present
  and configured with a distinct `lockName`).

## Notes

Advancing `nextOccurrenceDate` and inserting the `Transaction` should happen as two operations,
not one transaction — a failure between them means the series is retried next run and, worst case,
generates the same instance again next cycle before the advance lands. That's an acceptable
duplicate-generation window only if nothing downstream is idempotency-sensitive to it; if it's not
acceptable, insert first, then advance, and make advancing idempotent by checking
`nextOccurrenceDate` hasn't already moved past the instance just generated (compare against the
`_version` read at the start of this item's processing) before writing.
