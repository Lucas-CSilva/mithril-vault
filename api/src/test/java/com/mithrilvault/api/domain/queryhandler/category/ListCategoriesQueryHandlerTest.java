package com.mithrilvault.api.domain.queryhandler.category;

import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.fixture.model.Categories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ListCategoriesQueryHandlerTest {

  @Mock private CategoryReadRepository readRepository;

  private ListCategoriesQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ListCategoriesQueryHandler(readRepository);
  }

  @Test
  void returnsSystemAndOwnedCategories() {
    Category system = Categories.systemTopLevel();
    Category owned = Categories.userTopLevel();

    when(readRepository.findAllVisibleToOwner(Categories.DEFAULT_OWNER_ID))
        .thenReturn(Flux.just(system, owned));

    StepVerifier.create(handler.handle(Categories.DEFAULT_OWNER_ID))
        .expectNext(system)
        .expectNext(owned)
        .verifyComplete();
  }

  @Test
  void returnsEmptyFluxWhenNoCategories() {
    when(readRepository.findAllVisibleToOwner(Categories.DEFAULT_OWNER_ID))
        .thenReturn(Flux.empty());

    StepVerifier.create(handler.handle(Categories.DEFAULT_OWNER_ID)).verifyComplete();
  }
}
