package com.mithrilvault.api.domain.commandhandler.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.DomainException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import com.mithrilvault.api.fixture.command.category.CreateCategoryCommands;
import com.mithrilvault.api.fixture.model.Categories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateCategoryCommandHandlerTest {

  @Mock private CategoryRepository categoryRepository;
  @Mock private CategoryReadRepository categoryReadRepository;

  private CreateCategoryCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CreateCategoryCommandHandler(categoryRepository, categoryReadRepository);
  }

  @Test
  void savesTopLevelCategoryWithoutParent() {
    when(categoryRepository.save(any()))
        .thenAnswer(
            inv -> Mono.just(((Category) inv.getArgument(0)).toBuilder().id("new-id").build()));

    StepVerifier.create(
            handler.handle(CreateCategoryCommands.topLevel(), Categories.DEFAULT_OWNER_ID))
        .assertNext(
            cat -> {
              assertThat(cat.name()).isEqualTo(CreateCategoryCommands.DEFAULT_NAME);
              assertThat(cat.ownerId()).isEqualTo(Categories.DEFAULT_OWNER_ID);
              assertThat(cat.isSystem()).isFalse();
            })
        .verifyComplete();
  }

  @Test
  void savesSubcategoryWhenParentIsTopLevel() {
    Category parent = Categories.systemTopLevel();
    when(categoryReadRepository.findVisibleById(parent.id(), Categories.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(parent));
    when(categoryRepository.save(any()))
        .thenAnswer(
            inv -> Mono.just(((Category) inv.getArgument(0)).toBuilder().id("new-id").build()));

    StepVerifier.create(
            handler.handle(
                CreateCategoryCommands.withParent(parent.id()), Categories.DEFAULT_OWNER_ID))
        .assertNext(cat -> assertThat(cat.parentId()).isEqualTo(parent.id()))
        .verifyComplete();
  }

  @Test
  void rejectsSubcategoryWhenParentIsAlreadyAChild() {
    Category alreadyChild = Categories.userChild("some-parent-id");
    when(categoryReadRepository.findVisibleById(alreadyChild.id(), Categories.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(alreadyChild));

    StepVerifier.create(
            handler.handle(
                CreateCategoryCommands.withParent(alreadyChild.id()), Categories.DEFAULT_OWNER_ID))
        .expectError(DomainException.class)
        .verify();

    verify(categoryRepository, never()).save(any());
  }

  @Test
  void rejectsWhenParentNotVisible() {
    String ghostId = "ghost-parent-id";
    when(categoryReadRepository.findVisibleById(ghostId, Categories.DEFAULT_OWNER_ID))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle(CreateCategoryCommands.withParent(ghostId), Categories.DEFAULT_OWNER_ID))
        .expectError(DomainException.class)
        .verify();

    verify(categoryRepository, never()).save(any());
  }
}
