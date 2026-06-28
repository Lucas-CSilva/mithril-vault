package com.mithrilvault.api.domain.commandhandler.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;
import com.mithrilvault.api.domain.exception.ForbiddenException;
import com.mithrilvault.api.domain.exception.NotFoundException;
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
class UpdateCategoryCommandHandlerTest {

  @Mock private CategoryRepository categoryRepository;
  @Mock private CategoryReadRepository categoryReadRepository;

  private UpdateCategoryCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new UpdateCategoryCommandHandler(categoryRepository, categoryReadRepository);
  }

  @Test
  void updatesOwnedCategory() {
    Category existing =
        Category.builder().id("cat-1").name("Pets").ownerId("owner-1").isSystem(false).build();
    when(categoryReadRepository.findById("cat-1")).thenReturn(Mono.just(existing));
    when(categoryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(
            handler.handle(new UpdateCategoryCommand("cat-1", "Animals", null, null, "owner-1")))
        .assertNext(cat -> assertThat(cat.name()).isEqualTo("Animals"))
        .verifyComplete();
  }

  @Test
  void throwsForbiddenForSystemCategory() {
    Category system =
        Category.builder().id("sys-1").name("Alimentação").isSystem(true).ownerId(null).build();
    when(categoryReadRepository.findById("sys-1")).thenReturn(Mono.just(system));

    StepVerifier.create(
            handler.handle(new UpdateCategoryCommand("sys-1", "New Name", null, null, "owner-1")))
        .expectError(ForbiddenException.class)
        .verify();

    verify(categoryRepository, never()).save(any());
  }

  @Test
  void throwsNotFoundWhenCategoryBelongsToAnotherUser() {
    Category other =
        Category.builder().id("cat-1").name("Pets").ownerId("other-owner").isSystem(false).build();
    when(categoryReadRepository.findById("cat-1")).thenReturn(Mono.just(other));

    StepVerifier.create(
            handler.handle(new UpdateCategoryCommand("cat-1", "Stolen", null, null, "owner-1")))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void throwsNotFoundWhenCategoryDoesNotExist() {
    when(categoryReadRepository.findById("ghost")).thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle(new UpdateCategoryCommand("ghost", "X", null, null, "owner-1")))
        .expectError(NotFoundException.class)
        .verify();
  }
}
