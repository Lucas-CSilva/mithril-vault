package com.mithrilvault.api.integration.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.application.response.CategoryResponse;
import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.fixture.command.category.CreateCategoryCommands;
import com.mithrilvault.api.fixture.command.category.UpdateCategoryCommands;
import com.mithrilvault.api.infrastructure.persistence.CategoryMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class CategoryIT extends AbstractIntegrationTest {

  @Autowired private CategoryMongoRepository categoryMongoRepository;
  @Autowired private UserMongoRepository userMongoRepository;
  @Autowired private RefreshTokenMongoRepository refreshTokenMongoRepository;

  @BeforeEach
  void setUp() {
    categoryMongoRepository.deleteAll().block();
    userMongoRepository.deleteAll().block();
    refreshTokenMongoRepository.deleteAll().block();

    String accessToken = userSteps.createAndGetAccessToken();
    categorySteps.setAccessToken(accessToken);
  }

  // ── GET /categories ───────────────────────────────────────────────────

  @Test
  void listCategories_returns200_withOwnedAndSystemCategories() {
    categoryMongoRepository
        .save(CategoryDocument.builder().name("Alimentação").isSystem(true).build())
        .block();

    categorySteps.createAndGet(CreateCategoryCommands.topLevel());

    categorySteps
        .list()
        .expectStatus()
        .isOk()
        .expectBodyList(CategoryResponse.class)
        .value(
            list -> {
              assertThat(list).hasSize(2);
              assertThat(list)
                  .extracting(CategoryResponse::name)
                  .containsExactlyInAnyOrder("Alimentação", CreateCategoryCommands.DEFAULT_NAME);
            });
  }

  @Test
  void listCategories_returns401_whenUnauthenticated() {
    webTestClient.get().uri("/mithril-vault/categories").exchange().expectStatus().isUnauthorized();
  }

  // ── POST /categories ──────────────────────────────────────────────────

  @Test
  void createCategory_returns201_forValidTopLevelCategory() {
    categorySteps
        .create(CreateCategoryCommands.topLevel())
        .expectStatus()
        .isCreated()
        .expectBody(CategoryResponse.class)
        .value(
            body -> {
              assertThat(body.id()).isNotNull();
              assertThat(body.name()).isEqualTo(CreateCategoryCommands.DEFAULT_NAME);
              assertThat(body.isSystem()).isFalse();
            });
  }

  @Test
  void createCategory_returns201_forSubcategoryOfTopLevel() {
    CategoryResponse parent = categorySteps.createAndGet(CreateCategoryCommands.topLevel());

    categorySteps
        .create(CreateCategoryCommands.withParent(parent.id()))
        .expectStatus()
        .isCreated()
        .expectBody(CategoryResponse.class)
        .value(body -> assertThat(body.parentId()).isEqualTo(parent.id()));
  }

  @Test
  void createCategory_returns409_whenNameAlreadyExists() {
    categorySteps.createAndGet(CreateCategoryCommands.topLevel());

    categorySteps.create(CreateCategoryCommands.topLevel()).expectStatus().isEqualTo(409);
  }

  @Test
  void createCategory_returns422_whenNameIsBlank() {
    categorySteps.create(CreateCategoryCommands.withName("")).expectStatus().isEqualTo(422);
  }

  @Test
  void createCategory_returns401_whenUnauthenticated() {
    webTestClient
        .post()
        .uri("/mithril-vault/categories")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateCategoryCommands.topLevel())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  // ── PATCH /categories/{id} ────────────────────────────────────────────

  @Test
  void patchCategory_returns200_withUpdatedName() {
    CategoryResponse created = categorySteps.createAndGet(CreateCategoryCommands.topLevel());

    categorySteps
        .patch(created.id(), UpdateCategoryCommands.withName())
        .expectStatus()
        .isOk()
        .expectBody(CategoryResponse.class)
        .value(body -> assertThat(body.name()).isEqualTo(UpdateCategoryCommands.UPDATED_NAME));
  }

  @Test
  void patchCategory_returns403_forSystemCategory() {
    CategoryDocument systemDoc =
        categoryMongoRepository
            .save(CategoryDocument.builder().name("Alimentação").isSystem(true).build())
            .block();

    categorySteps
        .patch(systemDoc.getId(), UpdateCategoryCommands.withName())
        .expectStatus()
        .isForbidden();
  }

  @Test
  void patchCategory_returns404_forOtherUsersCategory() {
    CategoryDocument otherDoc =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Other Cat")
                    .isSystem(false)
                    .ownerId("completely-different-owner")
                    .build())
            .block();

    categorySteps
        .patch(otherDoc.getId(), UpdateCategoryCommands.withName())
        .expectStatus()
        .isNotFound();
  }

  @Test
  void patchCategory_returns404_whenCategoryDoesNotExist() {
    categorySteps
        .patch("non-existent-id", UpdateCategoryCommands.withName())
        .expectStatus()
        .isNotFound();
  }

  // ── DELETE /categories/{id} ───────────────────────────────────────────

  @Test
  void deleteCategory_returns204_andCategoryIsGone() {
    CategoryResponse created = categorySteps.createAndGet(CreateCategoryCommands.topLevel());

    categorySteps.delete(created.id()).expectStatus().isNoContent();

    categorySteps
        .list()
        .expectStatus()
        .isOk()
        .expectBodyList(CategoryResponse.class)
        .value(
            list ->
                assertThat(list)
                    .extracting(CategoryResponse::name)
                    .doesNotContain(CreateCategoryCommands.DEFAULT_NAME));
  }

  @Test
  void deleteCategory_returns403_forSystemCategory() {
    CategoryDocument systemDoc =
        categoryMongoRepository
            .save(CategoryDocument.builder().name("Alimentação").isSystem(true).build())
            .block();

    categorySteps.delete(systemDoc.getId()).expectStatus().isForbidden();
  }

  @Test
  void deleteCategory_returns404_forOtherUsersCategory() {
    CategoryDocument otherDoc =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Other Cat")
                    .isSystem(false)
                    .ownerId("completely-different-owner")
                    .build())
            .block();

    categorySteps.delete(otherDoc.getId()).expectStatus().isNotFound();
  }

  @Test
  void deleteCategory_returns404_whenCategoryDoesNotExist() {
    categorySteps.delete("non-existent-id").expectStatus().isNotFound();
  }
}
