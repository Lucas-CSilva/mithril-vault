package com.mithrilvault.api.infrastructure.scheduler;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.infrastructure.config.DistributedLock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Objects;
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
public class BalanceReconciliationJob {

  private final AppProperties appProperties;
  private final AccountRepository accountRepository;
  private final AccountReadRepository accountReadRepository;
  private final MeterRegistry meterRegistry;

  private Counter driftCounter;
  private Counter versionConflictCounter;

  @Builder(toBuilder = true)
  private record RunStats(
      AtomicInteger accounts,
      AtomicInteger corrected,
      AtomicInteger versionConflicts,
      AtomicInteger failed) {

    static RunStats empty() {
      return RunStats.builder()
          .accounts(new AtomicInteger())
          .corrected(new AtomicInteger())
          .versionConflicts(new AtomicInteger())
          .failed(new AtomicInteger())
          .build();
    }
  }

  @PostConstruct
  void init() {
    driftCounter =
        Counter.builder("reconciliation.drift.total")
            .tag("job", "balanceReconciliation")
            .description("Balance drift corrections self-healed by the reconciliation job")
            .register(meterRegistry);
    versionConflictCounter =
        Counter.builder("reconciliation.version_conflict.total")
            .tag("job", "balanceReconciliation")
            .description(
                "Self-heal writes that lost a version race with a concurrent balance update")
            .register(meterRegistry);
  }

  @Scheduled(cron = "${app.scheduler.balance-reconciliation.cron}")
  @DistributedLock(
      lockName = "'balanceReconciliation'",
      lockAtLeastFor = "PT5S",
      lockAtMostFor = "PT30M")
  public Mono<Void> execute() {
    LocalDate asOfDate = LocalDate.now(appProperties.scheduler().zone());
    RunStats stats = RunStats.empty();

    return accountReadRepository
        .findAllActive()
        .doOnNext(account -> stats.accounts().incrementAndGet())
        .flatMap(
            account -> reconcileBalance(account, asOfDate, stats),
            appProperties.scheduler().balanceReconciliation().concurrency())
        .then()
        .doOnSuccess(
            v ->
                log.info(
                    "Balance reconciliation run complete: {} accounts, {} corrected, {} version"
                        + " conflicts, {} failed",
                    stats.accounts().get(),
                    stats.corrected().get(),
                    stats.versionConflicts().get(),
                    stats.failed().get()));
  }

  private Mono<Void> reconcileBalance(Account account, LocalDate asOfDate, RunStats stats) {
    var retryConfig = appProperties.scheduler().balanceReconciliation();
    return correctBalanceDrift(account.id(), account.ownerId(), stats)
        .retryWhen(
            Retry.backoff(
                    retryConfig.conflictRetryMaxAttempts(), retryConfig.conflictRetryBackoff())
                .filter(ConflictException.class::isInstance)
                .doBeforeRetry(
                    signal ->
                        log.warn(
                            "Retrying self-heal for account {} after version conflict (attempt"
                                + " {}/{})",
                            account.id(),
                            signal.totalRetries() + 1,
                            retryConfig.conflictRetryMaxAttempts())))
        .onErrorResume(e -> handleReconcileFailure(account, asOfDate, e, stats));
  }

  private Mono<Void> correctBalanceDrift(String accountId, String ownerId, RunStats stats) {
    return accountRepository
        .findByIdAndOwnerId(accountId, ownerId)
        .flatMap(
            fresh ->
                accountReadRepository
                    .recomputeBalance(accountId, ownerId)
                    .filter(
                        recomputedBalance ->
                            !Objects.equals(recomputedBalance, fresh.currentBalance()))
                    .flatMap(
                        correctedBalance ->
                            accountRepository.save(
                                fresh.toBuilder().currentBalance(correctedBalance).build())))
        .doOnNext(
            saved -> {
              driftCounter.increment();
              stats.corrected().incrementAndGet();
              log.info("Self-healed balance drift for account {}", accountId);
            })
        .doOnError(
            ConflictException.class,
            e -> {
              versionConflictCounter.increment();
              stats.versionConflicts().incrementAndGet();
            })
        .then();
  }

  private Mono<Void> handleReconcileFailure(
      Account account, LocalDate asOfDate, Throwable e, RunStats stats) {
    stats.failed().incrementAndGet();
    log.error(
        "Reconciliation failed for account {} from user {} on asOfDate: {}",
        account.id(),
        account.ownerId(),
        asOfDate,
        e);

    return Mono.empty();
  }
}
