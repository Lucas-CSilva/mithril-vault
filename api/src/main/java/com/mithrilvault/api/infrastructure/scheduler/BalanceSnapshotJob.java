package com.mithrilvault.api.infrastructure.scheduler;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalanceSnapshot;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.BalanceSnapshotRepository;
import com.mithrilvault.api.infrastructure.config.DistributedLock;
import java.time.LocalDate;
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

  @Scheduled(cron = "${app.scheduler.balance-snapshot.cron}")
  @DistributedLock(lockName = "'balanceSnapshot'", lockAtLeastFor = "PT5S", lockAtMostFor = "PT30M")
  public Mono<Void> execute() {
    LocalDate asOfDate = LocalDate.now(appProperties.scheduler().zone());
    return accountRepository
        .findAllActive()
        .flatMap(
            account -> snapshotAccount(account, asOfDate),
            appProperties.scheduler().balanceSnapshot().concurrency())
        .then();
  }

  private Mono<Void> snapshotAccount(Account account, LocalDate asOfDate) {
    return accountRepository
        .computeSnapshot(account.id(), account.ownerId())
        .flatMap(
            checkpoint ->
                snapshotRepository.save(BalanceSnapshot.create(account, checkpoint, asOfDate)))
        .doOnError(
            e -> log.error("Snapshot failed for account {} for date {}", account.id(), asOfDate, e))
        .onErrorResume(e -> Mono.empty())
        .then();
  }
}
