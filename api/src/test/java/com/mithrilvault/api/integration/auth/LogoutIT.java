package com.mithrilvault.api.integration.auth;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LogoutIT extends AbstractIntegrationTest {

  @Autowired private UserMongoRepository userMongoRepository;

  @Autowired private RefreshTokenMongoRepository refreshTokenMongoRepository;

  @BeforeEach
  void setUp() {
    userMongoRepository.deleteAll().block();
    refreshTokenMongoRepository.deleteAll().block();
  }

  @Test
  void returns204() {
    String refreshToken = userSteps.createAndGetRefreshToken();

    webTestClient
        .post()
        .uri("/mithril-vault/logout")
        .cookie("refreshToken", refreshToken)
        .exchange()
        .expectStatus()
        .isNoContent();
  }
}
