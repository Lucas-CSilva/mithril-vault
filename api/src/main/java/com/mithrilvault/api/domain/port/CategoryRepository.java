package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Category;
import java.util.List;
import reactor.core.publisher.Mono;

public interface CategoryRepository {
  Mono<Category> save(Category category);

  Mono<Void> deleteWithReassignment(String categoryId, List<String> childIds, String outrosId);
}
