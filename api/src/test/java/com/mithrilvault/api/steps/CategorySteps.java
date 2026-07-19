package com.mithrilvault.api.steps;

import com.mithrilvault.api.application.response.CategoryResponse;
import com.mithrilvault.api.domain.command.category.CreateCategoryCommand;
import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@Setter
public class CategorySteps {

  private static final String BASE_URI = "/mithril-vault/categories";

  private WebTestClient webTestClient;
  private String accessToken;

  public void init(WebTestClient webTestClient) {
    this.webTestClient = webTestClient;
  }

  public WebTestClient.ResponseSpec list() {
    return webTestClient.get().uri(BASE_URI).cookie("accessToken", accessToken).exchange();
  }

  public WebTestClient.ResponseSpec create(CreateCategoryCommand command) {
    return webTestClient
        .post()
        .uri(BASE_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .cookie("accessToken", accessToken)
        .bodyValue(command)
        .exchange();
  }

  public WebTestClient.ResponseSpec patch(String id, UpdateCategoryCommand command) {
    return webTestClient
        .patch()
        .uri(BASE_URI + "/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .cookie("accessToken", accessToken)
        .bodyValue(command)
        .exchange();
  }

  public WebTestClient.ResponseSpec delete(String id) {
    return webTestClient
        .delete()
        .uri(BASE_URI + "/" + id)
        .cookie("accessToken", accessToken)
        .exchange();
  }

  public CategoryResponse createAndGet(CreateCategoryCommand command) {
    return create(command)
        .expectStatus()
        .isCreated()
        .expectBody(CategoryResponse.class)
        .returnResult()
        .getResponseBody();
  }
}
