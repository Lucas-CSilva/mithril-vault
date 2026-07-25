package com.mithrilvault.api.steps;

import com.mithrilvault.api.application.response.AccountResponse;
import com.mithrilvault.api.domain.command.account.CreateAccountCommand;
import com.mithrilvault.api.domain.command.account.ReconcileAccountCommand;
import com.mithrilvault.api.domain.command.account.UpdateAccountCommand;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@Setter
public class AccountSteps {

  private static final String BASE_URI = "/mithril-vault/accounts";

  private WebTestClient webTestClient;
  private String accessToken;

  public void init(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }

  public WebTestClient.ResponseSpec list(boolean includeInactive) {
    return webTestClient
        .get()
        .uri(BASE_URI + "?includeInactive=" + includeInactive)
        .cookie("accessToken", accessToken)
        .exchange();
  }

  public WebTestClient.ResponseSpec create(CreateAccountCommand command) {
    return webTestClient
        .post()
        .uri(BASE_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .cookie("accessToken", accessToken)
        .bodyValue(command)
        .exchange();
  }

  public WebTestClient.ResponseSpec get(String id) {
    return webTestClient
        .get()
        .uri(BASE_URI + "/" + id)
        .cookie("accessToken", accessToken)
        .exchange();
  }

  public WebTestClient.ResponseSpec patch(String id, UpdateAccountCommand command) {
    return webTestClient
        .patch()
        .uri(BASE_URI + "/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .cookie("accessToken", accessToken)
        .bodyValue(command)
        .exchange();
  }

  public WebTestClient.ResponseSpec deactivate(String id) {
    return webTestClient
        .delete()
        .uri(BASE_URI + "/" + id)
        .cookie("accessToken", accessToken)
        .exchange();
  }

  public WebTestClient.ResponseSpec reactivate(String id) {
    return webTestClient
        .post()
        .uri(BASE_URI + "/" + id + "/reactivate")
        .cookie("accessToken", accessToken)
        .exchange();
  }

  public WebTestClient.ResponseSpec reconcile(String id, ReconcileAccountCommand command) {
    return webTestClient
        .post()
        .uri(BASE_URI + "/" + id + "/reconcile")
        .contentType(MediaType.APPLICATION_JSON)
        .cookie("accessToken", accessToken)
        .bodyValue(command)
        .exchange();
  }

  public WebTestClient.ResponseSpec balanceHistory(String id) {
    return webTestClient
        .get()
        .uri(BASE_URI + "/" + id + "/balance-history")
        .cookie("accessToken", accessToken)
        .exchange();
  }

  public AccountResponse createAndGet(CreateAccountCommand command) {
    return create(command)
        .expectStatus()
        .isCreated()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();
  }
}
