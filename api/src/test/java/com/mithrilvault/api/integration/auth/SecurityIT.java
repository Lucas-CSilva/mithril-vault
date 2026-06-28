package com.mithrilvault.api.integration.auth;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class SecurityIT extends AbstractIntegrationTest {

  @Test
  void returns401_whenNoJwtToken() {
    webTestClient
        .get()
        .uri("/mithril-vault/some-protected-resource")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void returns200_whenValidJwtProvided() {
    webTestClient.get().uri("/mithril-vault/actuator/health").exchange().expectStatus().isOk();
  }
}
