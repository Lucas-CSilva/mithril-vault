package com.mithrilvault.api.domain.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import java.time.Duration;
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
}
