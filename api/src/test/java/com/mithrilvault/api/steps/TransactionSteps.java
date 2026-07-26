package com.mithrilvault.api.steps;

import com.mithrilvault.api.application.response.TransactionResponse;
import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@Setter
public class TransactionSteps {

  private static final String BASE_URI = "/mithril-vault/transactions";

  private WebTestClient webTestClient;
  private String accessToken;

  public void init(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }

  public WebTestClient.ResponseSpec create(CreateTransactionCommand command) {
    return webTestClient
        .post()
        .uri(BASE_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .cookie("accessToken", accessToken)
        .bodyValue(command)
        .exchange();
  }

  public TransactionResponse createAndGet(CreateTransactionCommand command) {
    return create(command)
        .expectStatus()
        .isCreated()
        .expectBody(TransactionResponse.class)
        .returnResult()
        .getResponseBody();
  }
}
