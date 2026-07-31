package com.mithrilvault.api.infrastructure.adapter.projection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.infrastructure.adapter.persistence.ProjectionLeaderRepositoryAdapter;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProjectionLeaderElectorTest {

  private static final String PROJECTION_NAME = "accountBalance";
  private static final String INSTANCE_ID = "instance-1";
  private static final Duration LEASE_TTL = Duration.ofSeconds(30);

  @Mock private ProjectionLeaderRepositoryAdapter leaderRepository;

  private ProjectionLeaderElector elector;

  @BeforeEach
  void setUp() {
    elector = new ProjectionLeaderElector(leaderRepository);
  }

  @Test
  void leadershipSignal_startsWithFalse_beforeFirstAcquisitionAttempt() {
    when(leaderRepository.tryAcquireOrRenew(eq(PROJECTION_NAME), eq(INSTANCE_ID), any()))
        .thenReturn(Mono.never());

    StepVerifier.create(elector.leadershipSignal(PROJECTION_NAME, INSTANCE_ID, LEASE_TTL))
        .expectNext(false)
        .then(() -> sleepMillis(300))
        .thenCancel()
        .verify();

    verify(leaderRepository).tryAcquireOrRenew(eq(PROJECTION_NAME), eq(INSTANCE_ID), any());
  }

  @Test
  void leadershipSignal_emitsTrue_whenLeaseIsAcquired() {
    when(leaderRepository.tryAcquireOrRenew(eq(PROJECTION_NAME), eq(INSTANCE_ID), any()))
        .thenReturn(Mono.just(true));

    StepVerifier.withVirtualTime(
            () -> elector.leadershipSignal(PROJECTION_NAME, INSTANCE_ID, LEASE_TTL))
        .expectNext(false)
        .expectNext(true)
        .thenCancel()
        .verify();
  }

  @Test
  void leadershipSignal_collapsesRepeatedSameStateTicks_viaDistinctUntilChanged() {
    when(leaderRepository.tryAcquireOrRenew(eq(PROJECTION_NAME), eq(INSTANCE_ID), any()))
        .thenReturn(Mono.just(true));

    StepVerifier.withVirtualTime(
            () -> elector.leadershipSignal(PROJECTION_NAME, INSTANCE_ID, LEASE_TTL))
        .expectNext(false)
        .expectNext(true)
        .expectNoEvent(LEASE_TTL.dividedBy(3).multipliedBy(3))
        .thenCancel()
        .verify();
  }

  @Test
  void leadershipSignal_resumesAsFalse_whenRepositoryErrors() {
    when(leaderRepository.tryAcquireOrRenew(eq(PROJECTION_NAME), eq(INSTANCE_ID), any()))
        .thenReturn(Mono.error(new RuntimeException("Mongo unavailable")));

    // First "false" comes from startWith(); the second is the real signal, resumed from the
    // repository error — waiting for it proves the error was actually swallowed, not propagated.
    StepVerifier.create(elector.leadershipSignal(PROJECTION_NAME, INSTANCE_ID, LEASE_TTL))
        .expectNext(false)
        .expectNext(false)
        .thenCancel()
        .verify();
  }

  private static void sleepMillis(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
