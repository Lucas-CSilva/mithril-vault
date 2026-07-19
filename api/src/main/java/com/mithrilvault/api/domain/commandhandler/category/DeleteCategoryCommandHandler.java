package com.mithrilvault.api.domain.commandhandler.category;

import com.mithrilvault.api.domain.config.SystemCategoryIds;
import com.mithrilvault.api.domain.exception.ForbiddenException;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DeleteCategoryCommandHandler {

  private final CategoryRepository categoryRepository;
  private final CategoryReadRepository categoryReadRepository;
  private final SystemCategoryIds systemCategoryIds;

  public Mono<Void> handle(String id, String ownerId) {
    return categoryReadRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Category not found")))
        .flatMap(
            existing -> {
              if (existing.isSystem()) {
                return Mono.error(new ForbiddenException("System categories cannot be deleted"));
              }

              if (!ownerId.equals(existing.ownerId())) {
                return Mono.error(new NotFoundException("Category not found"));
              }

              return categoryReadRepository
                  .findChildrenByParentId(id)
                  .map(Category::id)
                  .collectList()
                  .flatMap(
                      childIds ->
                          categoryRepository.deleteWithReassignment(
                              id, childIds, systemCategoryIds.getOutrosId()));
            });
  }
}
