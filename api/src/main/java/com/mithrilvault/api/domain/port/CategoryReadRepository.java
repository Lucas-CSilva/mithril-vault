package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Category;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CategoryReadRepository {
  Flux<Category> findAllVisibleToOwner(String ownerId);

  Mono<Category> findVisibleById(String id, String ownerId);

  Mono<Category> findById(String id);

  Flux<Category> findChildrenByParentId(String parentId);
}
