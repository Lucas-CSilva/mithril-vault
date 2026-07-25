package com.mithrilvault.api.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.fixture.model.Categories;
import com.mithrilvault.api.infrastructure.persistence.CategoryMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class CategoryRepositoryAdapterIT extends AbstractIntegrationTest {

  @Autowired private CategoryRepositoryAdapter adapter;
  @Autowired private CategoryMongoRepository categoryMongoRepository;

  @BeforeEach
  void cleanUp() {
    categoryMongoRepository.deleteAll().block();
  }

  // ── findAllVisibleToOwner ──────────────────────────────────────────────

  @Test
  void findAllVisibleToOwner_returnsSystemAndOwnedCategories_excludesOtherUsers() {
    categoryMongoRepository
        .save(CategoryDocument.builder().name("Alimentação").isSystem(true).build())
        .block();
    categoryMongoRepository
        .save(
            CategoryDocument.builder()
                .name("Pets")
                .isSystem(false)
                .ownerId(Categories.DEFAULT_OWNER_ID)
                .build())
        .block();
    categoryMongoRepository
        .save(
            CategoryDocument.builder()
                .name("Other's Cat")
                .isSystem(false)
                .ownerId(Categories.OTHER_OWNER_ID)
                .build())
        .block();

    StepVerifier.create(adapter.findAllVisibleToOwner(Categories.DEFAULT_OWNER_ID).collectList())
        .assertNext(
            list -> {
              assertThat(list).hasSize(2);
              assertThat(list)
                  .extracting(Category::name)
                  .containsExactlyInAnyOrder("Alimentação", "Pets");
            })
        .verifyComplete();
  }

  // ── findVisibleById ────────────────────────────────────────────────────

  @Test
  void findVisibleById_returnsOwnedCategory() {
    CategoryDocument doc =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Pets")
                    .isSystem(false)
                    .ownerId(Categories.DEFAULT_OWNER_ID)
                    .build())
            .block();

    StepVerifier.create(adapter.findVisibleById(doc.getId(), Categories.DEFAULT_OWNER_ID))
        .assertNext(c -> assertThat(c.name()).isEqualTo("Pets"))
        .verifyComplete();
  }

  @Test
  void findVisibleById_returnsSystemCategory_forAnyOwner() {
    CategoryDocument doc =
        categoryMongoRepository
            .save(CategoryDocument.builder().name("Alimentação").isSystem(true).build())
            .block();

    StepVerifier.create(adapter.findVisibleById(doc.getId(), Categories.DEFAULT_OWNER_ID))
        .assertNext(c -> assertThat(c.isSystem()).isTrue())
        .verifyComplete();
  }

  @Test
  void findVisibleById_returnsEmpty_forOtherUsersCategory() {
    CategoryDocument doc =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Other's Cat")
                    .isSystem(false)
                    .ownerId(Categories.OTHER_OWNER_ID)
                    .build())
            .block();

    StepVerifier.create(adapter.findVisibleById(doc.getId(), Categories.DEFAULT_OWNER_ID))
        .verifyComplete();
  }

  // ── findById ──────────────────────────────────────────────────────────

  @Test
  void findById_returnsAnyCategory_ignoringOwner() {
    CategoryDocument doc =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Other's Cat")
                    .isSystem(false)
                    .ownerId(Categories.OTHER_OWNER_ID)
                    .build())
            .block();

    StepVerifier.create(adapter.findById(doc.getId()))
        .assertNext(c -> assertThat(c.name()).isEqualTo("Other's Cat"))
        .verifyComplete();
  }

  @Test
  void findById_returnsEmpty_whenNotFound() {
    StepVerifier.create(adapter.findById("non-existent-id")).verifyComplete();
  }

  // ── findChildrenByParentId ─────────────────────────────────────────────

  @Test
  void findChildrenByParentId_returnsOnlyDirectChildren() {
    CategoryDocument parent =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Pets")
                    .isSystem(false)
                    .ownerId(Categories.DEFAULT_OWNER_ID)
                    .build())
            .block();
    categoryMongoRepository
        .save(
            CategoryDocument.builder()
                .name("Ração")
                .isSystem(false)
                .ownerId(Categories.DEFAULT_OWNER_ID)
                .parentId(parent.getId())
                .build())
        .block();
    categoryMongoRepository
        .save(
            CategoryDocument.builder()
                .name("Unrelated")
                .isSystem(false)
                .ownerId(Categories.DEFAULT_OWNER_ID)
                .build())
        .block();

    StepVerifier.create(adapter.findChildrenByParentId(parent.getId()).collectList())
        .assertNext(
            list -> {
              assertThat(list).hasSize(1);
              assertThat(list.get(0).name()).isEqualTo("Ração");
            })
        .verifyComplete();
  }

  // ── save ──────────────────────────────────────────────────────────────

  @Test
  void save_persistsCategory_andReturnsDomainModel() {
    Category toSave = Categories.userTopLevel().toBuilder().id(null).build();

    StepVerifier.create(adapter.save(toSave))
        .assertNext(
            saved -> {
              assertThat(saved.id()).isNotNull();
              assertThat(saved.name()).isEqualTo(Categories.userTopLevel().name());
              assertThat(saved.ownerId()).isEqualTo(Categories.DEFAULT_OWNER_ID);
            })
        .verifyComplete();
  }

  @Test
  void save_throwsConflictException_whenNameDuplicatedForSameOwner() {
    Category first = Categories.userTopLevel().toBuilder().id(null).build();
    adapter.save(first).block();

    Category duplicate = Categories.userTopLevel().toBuilder().id(null).build();

    StepVerifier.create(adapter.save(duplicate)).expectError(ConflictException.class).verify();
  }

  // ── deleteWithReassignment ─────────────────────────────────────────────

  @Test
  void deleteWithReassignment_deletesParentAndReassignsChildren() {
    String outrosId = "outros-test-id";

    CategoryDocument parent =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Pets")
                    .isSystem(false)
                    .ownerId(Categories.DEFAULT_OWNER_ID)
                    .build())
            .block();
    String parentId = parent.getId();

    CategoryDocument child1 =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Ração")
                    .isSystem(false)
                    .ownerId(Categories.DEFAULT_OWNER_ID)
                    .parentId(parentId)
                    .build())
            .block();
    CategoryDocument child2 =
        categoryMongoRepository
            .save(
                CategoryDocument.builder()
                    .name("Brinquedos")
                    .isSystem(false)
                    .ownerId(Categories.DEFAULT_OWNER_ID)
                    .parentId(parentId)
                    .build())
            .block();

    StepVerifier.create(
            adapter.deleteWithReassignment(
                parentId, List.of(child1.getId(), child2.getId()), outrosId))
        .verifyComplete();

    StepVerifier.create(categoryMongoRepository.findById(parentId)).verifyComplete();

    StepVerifier.create(categoryMongoRepository.findById(child1.getId()))
        .assertNext(c -> assertThat(c.getParentId()).isEqualTo(outrosId))
        .verifyComplete();

    StepVerifier.create(categoryMongoRepository.findById(child2.getId()))
        .assertNext(c -> assertThat(c.getParentId()).isEqualTo(outrosId))
        .verifyComplete();
  }
}
