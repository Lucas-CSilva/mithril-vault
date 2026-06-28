package com.mithrilvault.api.domain.queryhandler.category;

import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.query.category.ListCategoriesQuery;
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
  void returnsAllVisibleCategories() {
    Category system = Category.builder().id("sys-1").name("Alimentação").isSystem(true).build();
    Category owned =
        Category.builder().id("usr-1").name("Pets").isSystem(false).ownerId("owner-1").build();

    when(readRepository.findAllVisibleToOwner("owner-1")).thenReturn(Flux.just(system, owned));

    StepVerifier.create(handler.handle(new ListCategoriesQuery("owner-1")))
        .expectNext(system)
        .expectNext(owned)
        .verifyComplete();
  }

  @Test
  void returnsEmptyFluxWhenNoCategories() {
    when(readRepository.findAllVisibleToOwner("owner-1")).thenReturn(Flux.empty());

    StepVerifier.create(handler.handle(new ListCategoriesQuery("owner-1"))).verifyComplete();
  }
}
