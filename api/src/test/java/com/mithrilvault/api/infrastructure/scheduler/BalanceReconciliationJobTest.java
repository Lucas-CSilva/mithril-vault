package com.mithrilvault.api.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
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
class BalanceReconciliationJobTest {

  @Mock private AccountRepository accountRepository;
  @Mock private AccountReadRepository accountReadRepository;

  private MeterRegistry meterRegistry;
  private BalanceReconciliationJob job;

  @BeforeEach
  void setUp() {
    var schedulerConfig =
        new AppProperties.SchedulerConfig(
            new AppProperties.SchedulerJobConfig(CronExpression.parse("0 0 0 1 * *"), 16),
            new AppProperties.BalanceReconciliationJobConfig(
                CronExpression.parse("0 0 0 * * *"), 16, 3, Duration.ofMillis(200)),
            ZoneId.of("America/Sao_Paulo"));
    var appProperties = new AppProperties(null, null, null, null, schedulerConfig);

    meterRegistry = new SimpleMeterRegistry();
    job =
        new BalanceReconciliationJob(
            appProperties, accountRepository, accountReadRepository, meterRegistry);
    job.init();
  }

  private static Account account(String id, String ownerId, Long currentBalance) {
    return Account.builder()
        .id(id)
        .ownerId(ownerId)
        .currentBalance(currentBalance)
        .version(0L)
        .build();
  }

  private double driftTotal() {
    return meterRegistry.get("reconciliation.drift.total").counter().count();
  }

  private double versionConflictTotal() {
    return meterRegistry.get("reconciliation.version_conflict.total").counter().count();
  }

  @Test
  void selfHeals_whenRecomputedBalanceDiffersFromCurrent() {
    var account = account("account-1", "owner-1", 1_000L);

    when(accountReadRepository.findAllActive()).thenReturn(Flux.just(account));
    when(accountRepository.findByIdAndOwnerId("account-1", "owner-1"))
        .thenReturn(Mono.just(account));
    when(accountReadRepository.recomputeBalance("account-1", "owner-1"))
        .thenReturn(Mono.just(1_500L));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(job.execute()).verifyComplete();

    verify(accountRepository).save(argThatCurrentBalanceIs(1_500L));
    assertThat(driftTotal()).isEqualTo(1);
    assertThat(versionConflictTotal()).isEqualTo(0);
  }

  @Test
  void doesNotSave_whenRecomputedBalanceMatchesCurrent() {
    var account = account("account-1", "owner-1", 1_000L);

    when(accountReadRepository.findAllActive()).thenReturn(Flux.just(account));
    when(accountRepository.findByIdAndOwnerId("account-1", "owner-1"))
        .thenReturn(Mono.just(account));
    when(accountReadRepository.recomputeBalance("account-1", "owner-1"))
        .thenReturn(Mono.just(1_000L));

    StepVerifier.create(job.execute()).verifyComplete();

    verify(accountRepository, never()).save(any(Account.class));
    assertThat(driftTotal()).isEqualTo(0);
    assertThat(versionConflictTotal()).isEqualTo(0);
  }

  @Test
  void continuesRemainingAccounts_whenOneAccountFailsWithNonConflictError() {
    var failingAccount = account("account-1", "owner-1", 1_000L);
    var healthyAccount = account("account-2", "owner-2", 2_000L);

    when(accountReadRepository.findAllActive())
        .thenReturn(Flux.just(failingAccount, healthyAccount));
    when(accountRepository.findByIdAndOwnerId("account-1", "owner-1"))
        .thenReturn(Mono.error(new RuntimeException("boom")));
    when(accountRepository.findByIdAndOwnerId("account-2", "owner-2"))
        .thenReturn(Mono.just(healthyAccount));
    when(accountReadRepository.recomputeBalance("account-2", "owner-2"))
        .thenReturn(Mono.just(2_500L));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(job.execute()).verifyComplete();

    verify(accountRepository, times(1)).save(any(Account.class));
    assertThat(driftTotal()).isEqualTo(1);
    assertThat(versionConflictTotal()).isEqualTo(0);
  }

  @Test
  void healsWithinSameRun_whenConflictResolvesOnRetry() {
    var staleAccount = account("account-1", "owner-1", 1_000L);
    var freshAccount = account("account-1", "owner-1", 1_000L).toBuilder().version(1L).build();

    when(accountReadRepository.findAllActive()).thenReturn(Flux.just(staleAccount));
    when(accountRepository.findByIdAndOwnerId("account-1", "owner-1"))
        .thenReturn(Mono.just(staleAccount), Mono.just(freshAccount));
    when(accountReadRepository.recomputeBalance("account-1", "owner-1"))
        .thenReturn(Mono.just(1_500L));
    when(accountRepository.save(any(Account.class)))
        .thenReturn(Mono.error(new ConflictException("version conflict")))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(job.execute()).verifyComplete();

    verify(accountRepository, times(2)).save(any(Account.class));
    assertThat(driftTotal()).isEqualTo(1);
    assertThat(versionConflictTotal()).isEqualTo(1);
  }

  @Test
  void givesUp_afterExhaustingConflictRetries() {
    var account = account("account-1", "owner-1", 1_000L);

    when(accountReadRepository.findAllActive()).thenReturn(Flux.just(account));
    when(accountRepository.findByIdAndOwnerId("account-1", "owner-1"))
        .thenReturn(Mono.just(account));
    when(accountReadRepository.recomputeBalance("account-1", "owner-1"))
        .thenReturn(Mono.just(1_500L));
    when(accountRepository.save(any(Account.class)))
        .thenReturn(Mono.error(new ConflictException("version conflict")));

    StepVerifier.create(job.execute()).verifyComplete();

    // 1 initial attempt + 3 retries (MAX_CONFLICT_RETRIES from test config) = 4 lost races
    verify(accountRepository, times(4)).save(any(Account.class));
    assertThat(driftTotal()).isEqualTo(0);
    assertThat(versionConflictTotal()).isEqualTo(4);
  }

  @Test
  void noOp_whenNoActiveAccounts() {
    when(accountReadRepository.findAllActive()).thenReturn(Flux.empty());

    StepVerifier.create(job.execute()).verifyComplete();

    verify(accountRepository, never()).save(any(Account.class));
    assertThat(driftTotal()).isEqualTo(0);
    assertThat(versionConflictTotal()).isEqualTo(0);
  }

  private static Account argThatCurrentBalanceIs(Long expected) {
    return org.mockito.ArgumentMatchers.argThat(
        account -> account != null && expected.equals(account.currentBalance()));
  }
}
