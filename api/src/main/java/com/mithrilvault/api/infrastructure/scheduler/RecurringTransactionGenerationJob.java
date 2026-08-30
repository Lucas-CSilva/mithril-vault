package com.mithrilvault.api.infrastructure.scheduler;

import com.mithrilvault.api.domain.commandhandler.transaction.CreateTransactionCommandHandler;
import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.config.SchedulerJobStatus;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import com.mithrilvault.api.domain.port.RecurringSeriesReadRepository;
import com.mithrilvault.api.domain.port.RecurringSeriesRepository;
import com.mithrilvault.api.infrastructure.config.DistributedLock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionGenerationJob {

  private static final String JOB_NAME = "recurringGeneration";

  private final AppProperties appProperties;
  private final RecurringSeriesRepository recurringSeriesRepository;
  private final RecurringSeriesReadRepository recurringSeriesReadRepository;
  private final CreateTransactionCommandHandler createTransactionCommandHandler;
  private final SchedulerJobMetrics schedulerJobMetrics;

  @Builder(toBuilder = true)
  private record RunStats(
      AtomicInteger due,
      AtomicInteger generated,
      AtomicInteger versionConflicts,
      AtomicInteger failed) {

    static RunStats empty() {
      return RunStats.builder()
          .due(new AtomicInteger())
          .generated(new AtomicInteger())
          .versionConflicts(new AtomicInteger())
          .failed(new AtomicInteger())
          .build();
    }
  }

  @Scheduled(cron = "${app.scheduler.recurring-generation.cron}")
  @DistributedLock(
      lockName = "'recurringGeneration'",
      lockAtLeastFor = "PT5S",
      lockAtMostFor = "PT30M")
  public Mono<Void> execute() {
    LocalDate now = LocalDate.now(appProperties.scheduler().zone());
    RunStats stats = RunStats.empty();

    return recurringSeriesReadRepository
        .findDueSeries(now)
        .doOnNext(series -> stats.due().incrementAndGet())
        .flatMap(
            series -> generateTransactionFromSeries(series, stats),
            appProperties.scheduler().recurringGeneration().concurrency())
        .then()
        .doOnSuccess(
            v ->
                log.info(
                    "Recurring transaction generation run complete: {} due, {} generated, {} "
                        + "version conflicts, {} failed",
                    stats.due().get(),
                    stats.generated().get(),
                    stats.versionConflicts().get(),
                    stats.failed().get()));
  }

  private Mono<Void> generateTransactionFromSeries(
      RecurringTransactionSeries series, RunStats stats) {
    return createTransactionCommandHandler
        .handle(series.toCommand(), series.ownerId(), series.recurringSeriesId())
        .then()
        .then(Mono.defer(() -> advanceSeries(series, stats)))
        .onErrorResume(e -> handleGenerationFailure(series, e, stats));
  }

  private Mono<Void> advanceSeries(RecurringTransactionSeries series, RunStats stats) {
    var retryConfig = appProperties.scheduler().recurringGeneration();
    return recurringSeriesRepository
        .advance(series.id(), series.nextOccurrenceDate(), series.version())
        .doOnError(
            ConflictException.class,
            e -> {
              schedulerJobMetrics.recordOutcome(JOB_NAME, SchedulerJobStatus.CONFLICT);
              stats.versionConflicts().incrementAndGet();
            })
        .retryWhen(
            Retry.backoff(
                    retryConfig.conflictRetryMaxAttempts(), retryConfig.conflictRetryBackoff())
                .filter(ConflictException.class::isInstance)
                .doBeforeRetry(
                    signal ->
                        log.warn(
                            "Retrying self-heal for recurring series {} after version conflict"
                                + " (attempt {}/{})",
                            series.id(),
                            signal.totalRetries() + 1,
                            retryConfig.conflictRetryMaxAttempts())))
        .doOnSuccess(
            v -> {
              schedulerJobMetrics.recordOutcome(JOB_NAME, SchedulerJobStatus.GENERATED);
              stats.generated().incrementAndGet();
            });
  }

  private Mono<Void> handleGenerationFailure(
      RecurringTransactionSeries series, Throwable e, RunStats stats) {
    schedulerJobMetrics.recordOutcome(JOB_NAME, SchedulerJobStatus.FAILED);
    stats.failed().incrementAndGet();
    log.error(
        "Recurring generation failed for series {} owner {}", series.id(), series.ownerId(), e);

    return Mono.empty();
  }
}
