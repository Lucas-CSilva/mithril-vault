package com.mithrilvault.api.domain.commandhandler.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.config.SystemCategoryIds;
import com.mithrilvault.api.domain.exception.ForbiddenException;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import com.mithrilvault.api.fixture.model.Categories;
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

  private static final String OUTROS_ID = "outros-fixture-id";

  @Mock private CategoryRepository categoryRepository;
  @Mock private CategoryReadRepository categoryReadRepository;
  @Mock private SystemCategoryIds systemCategoryIds;

  private DeleteCategoryCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new DeleteCategoryCommandHandler(
            categoryRepository, categoryReadRepository, systemCategoryIds);
    lenient().when(systemCategoryIds.getOutrosId()).thenReturn(OUTROS_ID);
  }

  @Test
  void deletesOwnedCategoryAndReassignsChildren() {
    Category parent = Categories.userTopLevel();
    Category child = Categories.userChild(parent.id());

    when(categoryReadRepository.findById(parent.id())).thenReturn(Mono.just(parent));
    when(categoryReadRepository.findChildrenByParentId(parent.id())).thenReturn(Flux.just(child));
    when(categoryRepository.deleteWithReassignment(
            eq(parent.id()), eq(List.of(child.id())), eq(OUTROS_ID)))
        .thenReturn(Mono.empty());

    StepVerifier.create(handler.handle(parent.id(), Categories.DEFAULT_OWNER_ID)).verifyComplete();

    verify(categoryRepository).deleteWithReassignment(parent.id(), List.of(child.id()), OUTROS_ID);
  }

  @Test
  void throwsForbiddenForSystemCategory() {
    Category system = Categories.systemTopLevel();
    when(categoryReadRepository.findById(system.id())).thenReturn(Mono.just(system));

    StepVerifier.create(handler.handle(system.id(), Categories.DEFAULT_OWNER_ID))
        .expectError(ForbiddenException.class)
        .verify();

    verify(categoryRepository, never()).deleteWithReassignment(any(), anyList(), anyString());
  }

  @Test
  void throwsNotFoundWhenNotOwned() {
    Category other = Categories.userTopLevel(Categories.OTHER_OWNER_ID);
    when(categoryReadRepository.findById(other.id())).thenReturn(Mono.just(other));

    StepVerifier.create(handler.handle(other.id(), Categories.DEFAULT_OWNER_ID))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void throwsNotFoundWhenCategoryDoesNotExist() {
    when(categoryReadRepository.findById("missing-id")).thenReturn(Mono.empty());

    StepVerifier.create(handler.handle("missing-id", Categories.DEFAULT_OWNER_ID))
        .expectError(NotFoundException.class)
        .verify();

    verify(categoryRepository, never()).deleteWithReassignment(any(), any(), any());
  }
}
