package com.mithrilvault.api.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalanceSnapshot;
import com.mithrilvault.api.domain.model.TransactionAggregate;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.BalanceSnapshotRepository;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BalanceSnapshotJobTest {

  @Mock private AccountReadRepository accountRepository;
  @Mock private BalanceSnapshotRepository snapshotRepository;

  private BalanceSnapshotJob job;

  @BeforeEach
  void setUp() {
    var schedulerConfig =
        new AppProperties.BalanceSnapshotConfig(
            CronExpression.parse("0 0 0 1 * *"), 16, ZoneId.of("America/Sao_Paulo"));
    var appProperties = new AppProperties(null, null, null, null, schedulerConfig);

    job = new BalanceSnapshotJob(appProperties, accountRepository, snapshotRepository);
  }

  private static Account account(String id, String ownerId) {
    return Account.builder().id(id).ownerId(ownerId).build();
  }

  private static TransactionAggregate checkpoint() {
    return new TransactionAggregate(1_000L, "transaction-1", Instant.now());
  }

  @Test
  void savesOneSnapshotPerActiveAccount() {
    var accountOne = account("account-1", "owner-1");
    var accountTwo = account("account-2", "owner-2");

    when(accountRepository.findAllActive()).thenReturn(Flux.just(accountOne, accountTwo));
    when(accountRepository.computeSnapshot("account-1", "owner-1"))
        .thenReturn(Mono.just(checkpoint()));
    when(accountRepository.computeSnapshot("account-2", "owner-2"))
        .thenReturn(Mono.just(checkpoint()));
    when(snapshotRepository.save(any(BalanceSnapshot.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(job.execute()).verifyComplete();

    verify(snapshotRepository, times(2)).save(any(BalanceSnapshot.class));
  }

  @Test
  void continuesRemainingAccounts_whenOneAccountFailsToSnapshot() {
    var failingAccount = account("account-1", "owner-1");
    var healthyAccount = account("account-2", "owner-2");

    when(accountRepository.findAllActive()).thenReturn(Flux.just(failingAccount, healthyAccount));
    when(accountRepository.computeSnapshot("account-1", "owner-1"))
        .thenReturn(Mono.error(new RuntimeException("boom")));
    when(accountRepository.computeSnapshot("account-2", "owner-2"))
        .thenReturn(Mono.just(checkpoint()));
    when(snapshotRepository.save(any(BalanceSnapshot.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(job.execute()).verifyComplete();

    verify(snapshotRepository, times(1)).save(any(BalanceSnapshot.class));
  }

  @Test
  void skipsSave_whenAccountHasNoActiveAccounts() {
    when(accountRepository.findAllActive()).thenReturn(Flux.empty());

    StepVerifier.create(job.execute()).verifyComplete();

    verify(snapshotRepository, never()).save(any(BalanceSnapshot.class));
  }
}
