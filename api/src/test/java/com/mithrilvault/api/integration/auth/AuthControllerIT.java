package com.mithrilvault.api.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.AbstractIntegrationTest;
import com.mithrilvault.api.application.response.AuthResponse;
import com.mithrilvault.api.fixture.command.auth.LoginCommands;
import com.mithrilvault.api.fixture.command.user.RegisterUserCommands;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AuthControllerIT extends AbstractIntegrationTest {

  @Autowired private UserMongoRepository userMongoRepository;

  @Autowired private RefreshTokenMongoRepository refreshTokenMongoRepository;

  @BeforeEach
  void cleanUp() {
    userMongoRepository.deleteAll().block();
    refreshTokenMongoRepository.deleteAll().block();
  }

  @Test
  void register_returns201WithAuthResponseAndCookies() {
    webTestClient
        .post()
        .uri("/mithril-vault/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"email":"%s","rawPassword":"%s","displayName":"%s"}
            """
                .formatted(
                    RegisterUserCommands.DEFAULT_EMAIL,
                    RegisterUserCommands.DEFAULT_PASSWORD,
                    RegisterUserCommands.DEFAULT_DISPLAY_NAME))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .exists("Set-Cookie")
        .expectBody(AuthResponse.class)
        .value(
            body -> {
              assertThat(body.email()).isEqualTo(RegisterUserCommands.DEFAULT_EMAIL);
              assertThat(body.displayName()).isEqualTo(RegisterUserCommands.DEFAULT_DISPLAY_NAME);
            });
  }

  @Test
  void login_returns200WithCookies_whenCredentialsAreValid() {
    registerUser();

    webTestClient
        .post()
        .uri("/mithril-vault/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"email":"%s","rawPassword":"%s"}
            """
                .formatted(LoginCommands.DEFAULT_EMAIL, LoginCommands.DEFAULT_PASSWORD))
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists("Set-Cookie")
        .expectBody(AuthResponse.class)
        .value(body -> assertThat(body.email()).isEqualTo(LoginCommands.DEFAULT_EMAIL));
  }

  @Test
  void login_returns401_whenPasswordIsWrong() {
    registerUser();

    webTestClient
        .post()
        .uri("/mithril-vault/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"email":"%s","rawPassword":"wrongpassword"}
            """
                .formatted(LoginCommands.DEFAULT_EMAIL))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void refresh_returns200WithRotatedCookies_whenRefreshCookieIsValid() {
    WebTestClient.ResponseSpec registerResponse =
        webTestClient
            .post()
            .uri("/mithril-vault/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {"email":"%s","rawPassword":"%s","displayName":"%s"}
                """
                    .formatted(
                        RegisterUserCommands.DEFAULT_EMAIL,
                        RegisterUserCommands.DEFAULT_PASSWORD,
                        RegisterUserCommands.DEFAULT_DISPLAY_NAME))
            .exchange()
            .expectStatus()
            .isCreated();

    String refreshTokenValue =
        registerResponse
            .returnResult(AuthResponse.class)
            .getResponseHeaders()
            .get("Set-Cookie")
            .stream()
            .filter(c -> c.startsWith("refreshToken="))
            .findFirst()
            .map(c -> c.split(";")[0].substring("refreshToken=".length()))
            .orElseThrow(() -> new AssertionError("refreshToken cookie not found"));

    webTestClient
        .post()
        .uri("/mithril-vault/refresh")
        .cookie("refreshToken", refreshTokenValue)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists("Set-Cookie");
  }

  @Test
  void logout_returns204() {
    WebTestClient.ResponseSpec registerResponse =
        webTestClient
            .post()
            .uri("/mithril-vault/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {"email":"%s","rawPassword":"%s","displayName":"%s"}
                """
                    .formatted(
                        RegisterUserCommands.DEFAULT_EMAIL,
                        RegisterUserCommands.DEFAULT_PASSWORD,
                        RegisterUserCommands.DEFAULT_DISPLAY_NAME))
            .exchange()
            .expectStatus()
            .isCreated();

    String refreshTokenValue =
        registerResponse
            .returnResult(AuthResponse.class)
            .getResponseHeaders()
            .get("Set-Cookie")
            .stream()
            .filter(c -> c.startsWith("refreshToken="))
            .findFirst()
            .map(c -> c.split(";")[0].substring("refreshToken=".length()))
            .orElseThrow(() -> new AssertionError("refreshToken cookie not set by register/login"));

    webTestClient
        .post()
        .uri("/mithril-vault/logout")
        .cookie("refreshToken", refreshTokenValue)
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  void protectedEndpoint_returns401_whenNoJwtToken() {
    webTestClient
        .get()
        .uri("/mithril-vault/some-protected-resource")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void protectedEndpoint_returns200_whenValidJwtProvided() {
    webTestClient.get().uri("/mithril-vault/actuator/health").exchange().expectStatus().isOk();
  }

  private void registerUser() {
    webTestClient
        .post()
        .uri("/mithril-vault/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"email":"%s","rawPassword":"%s","displayName":"%s"}
            """
                .formatted(
                    RegisterUserCommands.DEFAULT_EMAIL,
                    RegisterUserCommands.DEFAULT_PASSWORD,
                    RegisterUserCommands.DEFAULT_DISPLAY_NAME))
        .exchange()
        .expectStatus()
        .isCreated();
  }
}
