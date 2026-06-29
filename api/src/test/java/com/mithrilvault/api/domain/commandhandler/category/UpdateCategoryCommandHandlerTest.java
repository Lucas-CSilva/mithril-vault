package com.mithrilvault.api.domain.commandhandler.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.ForbiddenException;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import com.mithrilvault.api.fixture.command.category.UpdateCategoryCommands;
import com.mithrilvault.api.fixture.model.Categories;
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
    Category existing = Categories.userTopLevel();
    when(categoryReadRepository.findById(existing.id())).thenReturn(Mono.just(existing));
    when(categoryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(
            handler.handle(
                existing.id(), UpdateCategoryCommands.withName(), Categories.DEFAULT_OWNER_ID))
        .assertNext(cat -> assertThat(cat.name()).isEqualTo(UpdateCategoryCommands.UPDATED_NAME))
        .verifyComplete();
  }

  @Test
  void throwsForbiddenForSystemCategory() {
    Category system = Categories.systemTopLevel();
    when(categoryReadRepository.findById(system.id())).thenReturn(Mono.just(system));

    StepVerifier.create(
            handler.handle(
                system.id(), UpdateCategoryCommands.withName(), Categories.DEFAULT_OWNER_ID))
        .expectError(ForbiddenException.class)
        .verify();

    verify(categoryRepository, never()).save(any());
  }

  @Test
  void throwsNotFoundWhenCategoryBelongsToAnotherUser() {
    Category other = Categories.userTopLevel(Categories.OTHER_OWNER_ID);
    when(categoryReadRepository.findById(other.id())).thenReturn(Mono.just(other));

    StepVerifier.create(
            handler.handle(
                other.id(), UpdateCategoryCommands.withName(), Categories.DEFAULT_OWNER_ID))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void throwsNotFoundWhenCategoryDoesNotExist() {
    when(categoryReadRepository.findById("ghost")).thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle("ghost", UpdateCategoryCommands.withName(), Categories.DEFAULT_OWNER_ID))
        .expectError(NotFoundException.class)
        .verify();
  }
}
