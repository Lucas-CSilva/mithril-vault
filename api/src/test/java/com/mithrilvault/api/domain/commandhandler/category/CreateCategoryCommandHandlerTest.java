package com.mithrilvault.api.domain.commandhandler.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.command.category.CreateCategoryCommand;
import com.mithrilvault.api.domain.exception.DomainException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
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
            handler.handle(new CreateCategoryCommand("Pets", null, "🐾", "#A3BE8C", "owner-1")))
        .assertNext(
            cat -> {
              assert cat.name().equals("Pets");
              assert cat.ownerId().equals("owner-1");
              assert !cat.isSystem();
            })
        .verifyComplete();
  }

  @Test
  void savesSubcategoryWhenParentIsTopLevel() {
    Category parent = Category.builder().id("parent-1").name("Alimentação").isSystem(true).build();
    when(categoryReadRepository.findVisibleById("parent-1", "owner-1"))
        .thenReturn(Mono.just(parent));
    when(categoryRepository.save(any()))
        .thenAnswer(
            inv -> Mono.just(((Category) inv.getArgument(0)).toBuilder().id("new-id").build()));

    StepVerifier.create(
            handler.handle(
                new CreateCategoryCommand("Orgânicos", "parent-1", null, null, "owner-1")))
        .assertNext(
            cat -> {
              assert cat.parentId().equals("parent-1");
            })
        .verifyComplete();
  }

  @Test
  void rejectsSubcategoryWhenParentIsAlreadyAChild() {
    Category alreadyChild =
        Category.builder().id("child-1").name("Delivery").parentId("some-parent").build();
    when(categoryReadRepository.findVisibleById("child-1", "owner-1"))
        .thenReturn(Mono.just(alreadyChild));

    StepVerifier.create(
            handler.handle(new CreateCategoryCommand("Sub", "child-1", null, null, "owner-1")))
        .expectError(DomainException.class)
        .verify();

    verify(categoryRepository, never()).save(any());
  }

  @Test
  void rejectsWhenParentNotVisible() {
    when(categoryReadRepository.findVisibleById("ghost-id", "owner-1")).thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle(new CreateCategoryCommand("Sub", "ghost-id", null, null, "owner-1")))
        .expectError(DomainException.class)
        .verify();
  }
}
