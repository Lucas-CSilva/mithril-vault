package com.mithrilvault.api.application.controller;

import com.mithrilvault.api.domain.command.category.CreateCategoryCommand;
import com.mithrilvault.api.domain.command.category.DeleteCategoryCommand;
import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;
import com.mithrilvault.api.domain.commandhandler.category.CreateCategoryCommandHandler;
import com.mithrilvault.api.domain.commandhandler.category.DeleteCategoryCommandHandler;
import com.mithrilvault.api.domain.commandhandler.category.UpdateCategoryCommandHandler;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.query.category.ListCategoriesQuery;
import com.mithrilvault.api.domain.queryhandler.category.ListCategoriesQueryHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping
@RequiredArgsConstructor
@RestController("/categories")
public class CategoryController {

  private final CreateCategoryCommandHandler createCategoryCommandHandler;
  private final UpdateCategoryCommandHandler updateCategoryCommandHandler;
  private final DeleteCategoryCommandHandler deleteCategoryCommandHandler;
  private final ListCategoriesQueryHandler listCategoriesQueryHandler;

  @GetMapping
  public Flux<Category> listCategories() {
    return listCategoriesQueryHandler.handle(new ListCategoriesQuery(""));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<Category> create(@RequestBody @Valid CreateCategoryCommand command) {
    return createCategoryCommandHandler.handle(command);
  }

  @PatchMapping("/{id}")
  public Mono<Category> patch(
      @PathVariable String id, @RequestBody @Valid UpdateCategoryCommand command) {
    return updateCategoryCommandHandler.handle(command);
  }

  @DeleteMapping("/{id}")
  public Mono<Void> delete(@PathVariable String id) {
    return deleteCategoryCommandHandler.handle(new DeleteCategoryCommand(id, ""));
  }
}
