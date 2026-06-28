package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Category;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CategoryReadRepository {
  /** Returns system categories (isSystem=true) + caller's own. Sorted: system first, then alpha. */
  Flux<Category> findAllVisibleToOwner(String ownerId);

  /** Returns a category visible to the caller: system OR owned. Used to validate parentId. */
  Mono<Category> findVisibleById(String id, String ownerId);

  /** Returns any category by id regardless of owner. Used for ownership/system checks. */
  Mono<Category> findById(String id);

  /** Returns direct children of a parent category. */
  Flux<Category> findChildrenByParentId(String parentId);
}
