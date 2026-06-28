package com.mithrilvault.api.integration.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.application.response.AuthResponse;
import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.fixture.command.user.RegisterUserCommands;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class RegisterIT extends AbstractIntegrationTest {

  @Autowired private UserMongoRepository userMongoRepository;

  @BeforeEach
  void setUp() {
    userMongoRepository.deleteAll().block();
  }

  @Test
  void returns201WithAuthResponseAndCookies() {
    webTestClient
        .post()
        .uri("/mithril-vault/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(RegisterUserCommands.valid())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .exists("Set-Cookie")
        .expectBody(AuthResponse.class)
        .value(
            body -> {
              assertThat(body).isNotNull();
              assertThat(body.email()).isEqualTo(RegisterUserCommands.DEFAULT_EMAIL);
              assertThat(body.displayName()).isEqualTo(RegisterUserCommands.DEFAULT_DISPLAY_NAME);
            });
  }
}
