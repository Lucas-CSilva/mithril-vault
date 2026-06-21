package com.mithrilvault.api.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.application.response.AuthResponse;
import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.fixture.command.auth.LoginCommands;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class LoginIT extends AbstractIntegrationTest {

  @Autowired private UserMongoRepository userMongoRepository;

  @Autowired private RefreshTokenMongoRepository refreshTokenMongoRepository;

  @BeforeEach
  void setUp() {
    userMongoRepository.deleteAll().block();
    refreshTokenMongoRepository.deleteAll().block();
  }

  @Test
  void returns200WithCookies_whenCredentialsAreValid() {
    // Given
    userSteps.create();

    // When & Then
    webTestClient
        .post()
        .uri("/mithril-vault/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(LoginCommands.valid())
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists("Set-Cookie")
        .expectBody(AuthResponse.class)
        .value(
            body -> {
              assertThat(body).isNotNull();
              assertThat(body.email()).isEqualTo(LoginCommands.DEFAULT_EMAIL);
            });
  }

  @Test
  void returns401_whenPasswordIsWrong() {
    // Given
    userSteps.create();

    // When & Then
    webTestClient
        .post()
        .uri("/mithril-vault/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(LoginCommands.withWrongPassword())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
