package com.mithrilvault.api.domain.commandhandler.category;

import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;
import com.mithrilvault.api.domain.exception.ForbiddenException;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UpdateCategoryCommandHandler {

  private final CategoryRepository categoryRepository;
  private final CategoryReadRepository categoryReadRepository;

  public Mono<Category> handle(UpdateCategoryCommand command) {
    return categoryReadRepository
        .findById(command.id())
        .switchIfEmpty(Mono.error(new NotFoundException("Category not found")))
        .flatMap(
            existing -> {
              if (existing.isSystem()) {
                return Mono.error(new ForbiddenException("System categories cannot be modified"));
              }

              if (!command.ownerId().equals(existing.ownerId())) {
                return Mono.error(new NotFoundException("Category not found"));
              }

              Category.CategoryBuilder updated = existing.toBuilder();

              Optional.ofNullable(command.name()).ifPresent(updated::name);
              Optional.ofNullable(command.icon()).ifPresent(updated::icon);
              Optional.ofNullable(command.color()).ifPresent(updated::color);

              return categoryRepository.save(updated.build());
            });
  }
}
