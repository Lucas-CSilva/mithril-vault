package com.mithrilvault.api.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ProjectionLeaderRepositoryAdapterIT extends AbstractIntegrationTest {

  // Deliberately distinct from the real "accountBalance" projection name used by the
  // production AccountBalanceChangeStreamListener singleton, which is also live in this
  // shared IT context — reusing its name would race with its own lease acquisition.
  private static final String PROJECTION_NAME = "test-leader-projection";
  private static final Duration LEASE_TTL = Duration.ofSeconds(30);

  @Autowired private ProjectionLeaderRepositoryAdapter adapter;
  @Autowired private ReactiveMongoTemplate reactiveMongoTemplate;

  @BeforeEach
  void cleanUp() {
    // Scoped to our own PROJECTION_NAME (not a blanket collection wipe) so we don't disturb the
    // real production listener's own "accountBalance" lease, which is also live in this context.
    reactiveMongoTemplate
        .remove(Query.query(Criteria.where("_id").is(PROJECTION_NAME)), "projection_leases")
        .block();
  }

  @Test
  void tryAcquireOrRenew_acquiresUnclaimedLease() {
    StepVerifier.create(adapter.tryAcquireOrRenew(PROJECTION_NAME, "instance-a", LEASE_TTL))
        .assertNext(acquired -> assertThat(acquired).isTrue())
        .verifyComplete();
  }

  @Test
  void tryAcquireOrRenew_losesRace_whenAnotherInstanceHoldsAnUnexpiredLease() {
    adapter.tryAcquireOrRenew(PROJECTION_NAME, "instance-a", LEASE_TTL).block();

    StepVerifier.create(adapter.tryAcquireOrRenew(PROJECTION_NAME, "instance-b", LEASE_TTL))
        .assertNext(acquired -> assertThat(acquired).isFalse())
        .verifyComplete();
  }

  @Test
  void tryAcquireOrRenew_currentHolderCanRenewItsOwnLease() {
    adapter.tryAcquireOrRenew(PROJECTION_NAME, "instance-a", LEASE_TTL).block();

    StepVerifier.create(adapter.tryAcquireOrRenew(PROJECTION_NAME, "instance-a", LEASE_TTL))
        .assertNext(acquired -> assertThat(acquired).isTrue())
        .verifyComplete();
  }

  @Test
  void tryAcquireOrRenew_anotherInstanceCanStealAnExpiredLease() {
    adapter.tryAcquireOrRenew(PROJECTION_NAME, "instance-a", Duration.ofMillis(1)).block();

    Mono<Boolean> stealAfterExpiry =
        Mono.delay(Duration.ofMillis(50))
            .then(adapter.tryAcquireOrRenew(PROJECTION_NAME, "instance-b", LEASE_TTL));

    StepVerifier.create(stealAfterExpiry)
        .assertNext(acquired -> assertThat(acquired).isTrue())
        .verifyComplete();
  }
}
