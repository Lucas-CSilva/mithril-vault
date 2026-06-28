package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Category;
import java.util.List;
import reactor.core.publisher.Mono;

public interface CategoryRepository {
  Mono<Category> save(Category category);

  /**
   * Atomically: updateMany(transactions.categoryId in allIds → outrosId), then
   * deleteMany(children), then deleteOne(category). Runs in a MongoDB transaction.
   */
  Mono<Void> deleteWithReassignment(String categoryId, List<String> childIds, String outrosId);
}
