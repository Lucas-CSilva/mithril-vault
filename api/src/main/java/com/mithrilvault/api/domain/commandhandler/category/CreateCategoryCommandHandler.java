package com.mithrilvault.api.domain.commandhandler.category;

import com.mithrilvault.api.domain.command.category.CreateCategoryCommand;
import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.exception.ErrorCode;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreateCategoryCommandHandler {

  private final CategoryRepository categoryRepository;
  private final CategoryReadRepository categoryReadRepository;

  public Mono<Category> handle(CreateCategoryCommand command) {
    if (command.parentId() == null) {
      return save(command, null);
    }
    return categoryReadRepository
        .findVisibleById(command.parentId(), command.ownerId())
        .switchIfEmpty(Mono.error(new NotFoundException("Parent category not found")))
        .flatMap(
            parent -> {
              if (parent.parentId() != null) {
                return Mono.error(
                    new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Cannot create a subcategory of a subcategory (max depth = 1)"));
              }
              return save(command, parent.id());
            });
  }

  private Mono<Category> save(CreateCategoryCommand command, String resolvedParentId) {
    Category category =
        Category.builder()
            .id(UUID.randomUUID().toString())
            .name(command.name())
            .parentId(resolvedParentId)
            .icon(command.icon())
            .color(command.color())
            .isSystem(false)
            .ownerId(command.ownerId())
            .build();
    return categoryRepository.save(category);
  }
}
