package com.mithrilvault.api.domain.commandhandler.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.command.category.DeleteCategoryCommand;
import com.mithrilvault.api.domain.config.SystemCategoryIds;
import com.mithrilvault.api.domain.exception.ForbiddenException;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeleteCategoryCommandHandlerTest {

  @Mock private CategoryRepository categoryRepository;
  @Mock private CategoryReadRepository categoryReadRepository;
  @Mock private SystemCategoryIds systemCategoryIds;

  private DeleteCategoryCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new DeleteCategoryCommandHandler(
            categoryRepository, categoryReadRepository, systemCategoryIds);
    lenient().when(systemCategoryIds.getOutrosId()).thenReturn("outros-id");
  }

  @Test
  void deletesOwnedCategoryAndItsChildren() {
    Category parent =
        Category.builder().id("cat-1").name("Pets").ownerId("owner-1").isSystem(false).build();
    Category child =
        Category.builder()
            .id("child-1")
            .name("Ração")
            .parentId("cat-1")
            .ownerId("owner-1")
            .isSystem(false)
            .build();

    when(categoryReadRepository.findById("cat-1")).thenReturn(Mono.just(parent));
    when(categoryReadRepository.findChildrenByParentId("cat-1")).thenReturn(Flux.just(child));
    when(categoryRepository.deleteWithReassignment(
            eq("cat-1"), eq(List.of("child-1")), eq("outros-id")))
        .thenReturn(Mono.empty());

    StepVerifier.create(handler.handle(new DeleteCategoryCommand("cat-1", "owner-1")))
        .verifyComplete();

    verify(categoryRepository).deleteWithReassignment("cat-1", List.of("child-1"), "outros-id");
  }

  @Test
  void throwsForbiddenForSystemCategory() {
    Category system = Category.builder().id("sys-1").name("Alimentação").isSystem(true).build();
    when(categoryReadRepository.findById("sys-1")).thenReturn(Mono.just(system));

    StepVerifier.create(handler.handle(new DeleteCategoryCommand("sys-1", "owner-1")))
        .expectError(ForbiddenException.class)
        .verify();

    verify(categoryRepository, never()).deleteWithReassignment(any(), anyList(), anyString());
  }

  @Test
  void throwsNotFoundWhenNotOwned() {
    Category other =
        Category.builder().id("cat-1").name("X").ownerId("other").isSystem(false).build();
    when(categoryReadRepository.findById("cat-1")).thenReturn(Mono.just(other));

    StepVerifier.create(handler.handle(new DeleteCategoryCommand("cat-1", "owner-1")))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void throwsNotFoundWhenCategoryDoesNotExist() {
    when(categoryReadRepository.findById("missing-id")).thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle(new DeleteCategoryCommand("missing-id", "owner-1")))
        .expectError(NotFoundException.class)
        .verify();

    verify(categoryRepository, never()).deleteWithReassignment(any(), any(), any());
  }
}
