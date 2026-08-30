package com.mithrilvault.api.domain.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AppPropertiesIT extends AbstractIntegrationTest {

  @Autowired private AppProperties appProperties;

  @Test
  void leaderTtl_bindsAsThirtySeconds_notMilliseconds() {
    assertThat(appProperties.leader().ttl()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void leaderRetryBackoff_bindsAsThreeHundredMilliseconds() {
    assertThat(appProperties.leader().retryBackoff()).isEqualTo(Duration.ofMillis(300));
  }

  @Test
  void leaderMaxRetries_bindsAsThree() {
    assertThat(appProperties.leader().maxRetries()).isEqualTo(3);
  }

  @Test
  void zone_bindsAsSaoPaulo() {
    assertThat(appProperties.zone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
  }

  @Test
  void balanceSnapshotCron_parsesAndBindsConcurrency() {
    assertThat(appProperties.scheduler().balanceSnapshot().cron().toString())
        .isEqualTo("0 0 0 1 * *");
    assertThat(appProperties.scheduler().balanceSnapshot().concurrency()).isEqualTo(16);
  }

  @Test
  void balanceReconciliationCron_parsesAndBindsConcurrency() {
    assertThat(appProperties.scheduler().balanceReconciliation().cron().toString())
        .isEqualTo("0 0 0 * * *");
    assertThat(appProperties.scheduler().balanceReconciliation().concurrency()).isEqualTo(16);
  }

  @Test
  void balanceReconciliationConflictRetry_bindsMaxAttemptsAndBackoff() {
    assertThat(appProperties.scheduler().balanceReconciliation().conflictRetryMaxAttempts())
        .isEqualTo(3);
    assertThat(appProperties.scheduler().balanceReconciliation().conflictRetryBackoff())
        .isEqualTo(Duration.ofMillis(200));
  }
}
