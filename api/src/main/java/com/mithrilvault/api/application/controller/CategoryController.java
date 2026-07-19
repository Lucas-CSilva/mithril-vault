package com.mithrilvault.api.application.controller;

import com.mithrilvault.api.application.mapper.CategoryResponseMapper;
import com.mithrilvault.api.application.response.CategoryResponse;
import com.mithrilvault.api.domain.command.category.CreateCategoryCommand;
import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;
import com.mithrilvault.api.domain.commandhandler.category.CreateCategoryCommandHandler;
import com.mithrilvault.api.domain.commandhandler.category.DeleteCategoryCommandHandler;
import com.mithrilvault.api.domain.commandhandler.category.UpdateCategoryCommandHandler;
import com.mithrilvault.api.domain.queryhandler.category.ListCategoriesQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
  private final CategoryResponseMapper categoryResponseMapper;
  private final ListCategoriesQueryHandler listCategoriesQueryHandler;
  private final CreateCategoryCommandHandler createCategoryCommandHandler;
  private final UpdateCategoryCommandHandler updateCategoryCommandHandler;
  private final DeleteCategoryCommandHandler deleteCategoryCommandHandler;

  @GetMapping
  public Flux<CategoryResponse> listCategories(
      @AuthenticationPrincipal(expression = "subject") String ownerId) {
    return listCategoriesQueryHandler.handle(ownerId).map(categoryResponseMapper::toResponse);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<CategoryResponse> create(
      @AuthenticationPrincipal(expression = "subject") String ownerId,
      @RequestBody @Valid CreateCategoryCommand command) {
    return createCategoryCommandHandler
        .handle(command, ownerId)
        .map(categoryResponseMapper::toResponse);
  }

  @PatchMapping("/{id}")
  public Mono<CategoryResponse> patch(
      @PathVariable String id,
      @AuthenticationPrincipal(expression = "subject") String ownerId,
      @RequestBody @Valid UpdateCategoryCommand command) {
    return updateCategoryCommandHandler
        .handle(id, command, ownerId)
        .map(categoryResponseMapper::toResponse);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(
      @PathVariable String id, @AuthenticationPrincipal(expression = "subject") String ownerId) {
    return deleteCategoryCommandHandler.handle(id, ownerId);
  }
}
