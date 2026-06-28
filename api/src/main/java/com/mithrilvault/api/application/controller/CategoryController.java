package com.mithrilvault.api.application.controller;

import com.mithrilvault.api.application.mapper.CategoryResponseMapper;
import com.mithrilvault.api.application.response.CategoryResponse;
import com.mithrilvault.api.domain.command.category.CreateCategoryCommand;
import com.mithrilvault.api.domain.command.category.DeleteCategoryCommand;
import com.mithrilvault.api.domain.command.category.UpdateCategoryCommand;
import com.mithrilvault.api.domain.commandhandler.category.CreateCategoryCommandHandler;
import com.mithrilvault.api.domain.commandhandler.category.DeleteCategoryCommandHandler;
import com.mithrilvault.api.domain.commandhandler.category.UpdateCategoryCommandHandler;
import com.mithrilvault.api.domain.query.category.ListCategoriesQuery;
import com.mithrilvault.api.domain.queryhandler.category.ListCategoriesQueryHandler;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/mithril-vault/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CreateCategoryCommandHandler createCategoryCommandHandler;
  private final UpdateCategoryCommandHandler updateCategoryCommandHandler;
  private final DeleteCategoryCommandHandler deleteCategoryCommandHandler;
  private final ListCategoriesQueryHandler listCategoriesQueryHandler;
  private final CategoryResponseMapper categoryResponseMapper;

  @GetMapping
  public Mono<ResponseEntity<List<CategoryResponse>>> listCategories(
      @AuthenticationPrincipal(expression = "subject") String ownerId) {
    return listCategoriesQueryHandler
        .handle(new ListCategoriesQuery(ownerId))
        .map(categoryResponseMapper::toResponse)
        .collectList()
        .map(ResponseEntity::ok);
  }

  @PostMapping
  public Mono<ResponseEntity<CategoryResponse>> create(
      @AuthenticationPrincipal(expression = "subject") String ownerId,
      @RequestBody @Valid CreateCategoryCommand command) {
    return createCategoryCommandHandler
        .handle(
            new CreateCategoryCommand(
                command.name(), command.parentId(), command.icon(), command.color(), ownerId))
        .map(categoryResponseMapper::toResponse)
        .map(resp -> ResponseEntity.status(HttpStatus.CREATED).body(resp));
  }

  @PatchMapping("/{id}")
  public Mono<ResponseEntity<CategoryResponse>> patch(
      @PathVariable String id,
      @AuthenticationPrincipal(expression = "subject") String ownerId,
      @RequestBody @Valid UpdateCategoryCommand command) {
    return updateCategoryCommandHandler
        .handle(
            new UpdateCategoryCommand(id, command.name(), command.icon(), command.color(), ownerId))
        .map(categoryResponseMapper::toResponse)
        .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> delete(
      @PathVariable String id, @AuthenticationPrincipal(expression = "subject") String ownerId) {
    return deleteCategoryCommandHandler
        .handle(new DeleteCategoryCommand(id, ownerId))
        .then(Mono.just(ResponseEntity.<Void>noContent().build()));
  }
}
