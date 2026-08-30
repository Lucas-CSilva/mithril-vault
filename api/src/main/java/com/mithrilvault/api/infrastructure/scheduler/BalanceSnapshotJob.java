package com.mithrilvault.api.infrastructure.scheduler;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.config.SchedulerJobStatus;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalanceSnapshot;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.BalanceSnapshotRepository;
import com.mithrilvault.api.infrastructure.config.DistributedLock;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceSnapshotJob {
  private static final String JOB_NAME = "balanceSnapshot";

  private final AppProperties appProperties;
  private final Clock clock;
  private final AccountReadRepository accountRepository;
  private final BalanceSnapshotRepository snapshotRepository;
  private final SchedulerJobMetrics schedulerJobMetrics;

  @Builder(toBuilder = true)
  private record RunStats(AtomicInteger accounts, AtomicInteger saved, AtomicInteger failed) {

    static RunStats empty() {
      return RunStats.builder()
          .accounts(new AtomicInteger())
          .saved(new AtomicInteger())
          .failed(new AtomicInteger())
          .build();
    }
  }

  @Scheduled(cron = "${app.scheduler.balance-snapshot.cron}")
  @DistributedLock(lockName = "'balanceSnapshot'", lockAtLeastFor = "PT5S", lockAtMostFor = "PT30M")
  public Mono<Void> execute() {
    LocalDate asOfDate = LocalDate.now(clock);
    RunStats stats = RunStats.empty();

    return accountRepository
        .findAllActive()
        .doOnNext(account -> stats.accounts().incrementAndGet())
        .flatMap(
            account -> snapshotAccount(account, asOfDate, stats),
            appProperties.scheduler().balanceSnapshot().concurrency())
        .then()
        .doOnSuccess(
            v ->
                log.info(
                    "Balance snapshot run complete: {} accounts, {} saved, {} failed",
                    stats.accounts().get(),
                    stats.saved().get(),
                    stats.failed().get()));
  }

  private Mono<Void> snapshotAccount(Account account, LocalDate asOfDate, RunStats stats) {
    return accountRepository
        .computeSnapshot(account.id(), account.ownerId())
        .flatMap(
            checkpoint ->
                snapshotRepository.save(BalanceSnapshot.create(account, checkpoint, asOfDate)))
        .doOnNext(
            saved -> {
              schedulerJobMetrics.recordOutcome(JOB_NAME, SchedulerJobStatus.SAVED);
              stats.saved().incrementAndGet();
            })
        .doOnError(
            e -> log.error("Snapshot failed for account {} for date {}", account.id(), asOfDate, e))
        .onErrorResume(
            e -> {
              schedulerJobMetrics.recordOutcome(JOB_NAME, SchedulerJobStatus.FAILED);
              stats.failed().incrementAndGet();
              return Mono.empty();
            })
        .then();
  }
}
