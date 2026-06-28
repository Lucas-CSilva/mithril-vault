package com.mithrilvault.api.domain.commandhandler.category;

import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;
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
              Category updated =
                  existing.toBuilder()
                      .name(command.name() != null ? command.name() : existing.name())
                      .icon(command.icon() != null ? command.icon() : existing.icon())
                      .color(command.color() != null ? command.color() : existing.color())
                      .build();
              return categoryRepository.save(updated);
            });
  }
}
