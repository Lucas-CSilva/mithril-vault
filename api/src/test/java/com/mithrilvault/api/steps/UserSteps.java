package com.mithrilvault.api.steps;

import com.mithrilvault.api.application.response.AuthResponse;
import com.mithrilvault.api.fixture.command.user.RegisterUserCommands;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

public class UserSteps {

  private WebTestClient webTestClient;

  public void init(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }

  public AuthResponse create() {
    return webTestClient
        .post()
        .uri("/mithril-vault/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(RegisterUserCommands.valid())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(AuthResponse.class)
        .returnResult()
        .getResponseBody();
  }

  public String createAndGetAccessToken() {
    return webTestClient
        .post()
        .uri("/mithril-vault/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(RegisterUserCommands.valid())
        .exchange()
        .expectStatus()
        .isCreated()
        .returnResult(AuthResponse.class)
        .getResponseHeaders()
        .get("Set-Cookie")
        .stream()
        .filter(c -> c.startsWith("accessToken="))
        .findFirst()
        .map(c -> c.split(";")[0].substring("accessToken=".length()))
        .orElseThrow(() -> new AssertionError("accessToken cookie not found"));
  }

  public String createAndGetRefreshToken() {
    return webTestClient
        .post()
        .uri("/mithril-vault/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(RegisterUserCommands.valid())
        .exchange()
        .expectStatus()
        .isCreated()
        .returnResult(AuthResponse.class)
        .getResponseHeaders()
        .get("Set-Cookie")
        .stream()
        .filter(c -> c.startsWith("refreshToken="))
        .findFirst()
        .map(c -> c.split(";")[0].substring("refreshToken=".length()))
        .orElseThrow(() -> new AssertionError("refreshToken cookie not found"));
  }
}
