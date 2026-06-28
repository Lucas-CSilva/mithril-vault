package com.mithrilvault.api.domain.queryhandler.category;

import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.query.category.ListCategoriesQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ListCategoriesQueryHandler {

  private final CategoryReadRepository readRepository;

  public Flux<Category> handle(ListCategoriesQuery query) {
    return readRepository.findAllVisibleToOwner(query.ownerId());
  }
}
