package com.mithrilvault.api.infrastructure.scheduler;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalanceSnapshot;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.BalanceSnapshotRepository;
import com.mithrilvault.api.infrastructure.config.DistributedLock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
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
  private final AppProperties appProperties;
  private final AccountReadRepository accountRepository;
  private final BalanceSnapshotRepository snapshotRepository;
  private final MeterRegistry meterRegistry;

  private Counter savedCounter;
  private Counter failedCounter;

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

  @PostConstruct
  void init() {
    savedCounter =
        Counter.builder("balance-snapshot.saved.total")
            .tag("job", "balanceSnapshot")
            .description("Balance snapshots successfully checkpointed")
            .register(meterRegistry);
    failedCounter =
        Counter.builder("balance-snapshot.failed.total")
            .tag("job", "balanceSnapshot")
            .description("Accounts whose snapshot failed to checkpoint this cycle")
            .register(meterRegistry);
  }

  @Scheduled(cron = "${app.scheduler.balance-snapshot.cron}")
  @DistributedLock(lockName = "'balanceSnapshot'", lockAtLeastFor = "PT5S", lockAtMostFor = "PT30M")
  public Mono<Void> execute() {
    LocalDate asOfDate = LocalDate.now(appProperties.scheduler().zone());
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
              savedCounter.increment();
              stats.saved().incrementAndGet();
            })
        .doOnError(
            e -> log.error("Snapshot failed for account {} for date {}", account.id(), asOfDate, e))
        .onErrorResume(
            e -> {
              failedCounter.increment();
              stats.failed().incrementAndGet();
              return Mono.empty();
            })
        .then();
  }
}
