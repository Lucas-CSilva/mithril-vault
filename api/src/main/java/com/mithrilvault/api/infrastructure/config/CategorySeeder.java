package com.mithrilvault.api.infrastructure.config;

import com.mithrilvault.api.domain.config.SystemCategoryIds;
import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

  private static final List<String> SYSTEM_CATEGORY_NAMES =
      List.of(
          "Alimentação",
          "Moradia",
          "Transporte",
          "Saúde",
          "Educação",
          "Lazer",
          "Vestuário",
          "Serviços & Assinaturas",
          "Investimentos",
          "Transferências",
          "Renda",
          "Outros");

  private final ReactiveMongoTemplate mongoTemplate;
  private final SystemCategoryIds systemCategoryIds;

  @Override
  public void run(ApplicationArguments args) {
    Flux.fromIterable(SYSTEM_CATEGORY_NAMES).flatMap(this::seedCategory).then().block();
  }

  private Mono<CategoryDocument> seedCategory(String name) {
    Query query = Query.query(Criteria.where("isSystem").is(true).and("name").is(name));
    return mongoTemplate
        .findOne(query, CategoryDocument.class)
        .switchIfEmpty(Mono.defer(() -> mongoTemplate.save(buildDocument(name))));
  }

  private CategoryDocument buildDocument(String name) {
    if ("Outros".equals(name)) {
      return CategoryDocument.builder()
          .id(systemCategoryIds.getOutrosId())
          .name(name)
          .isSystem(true)
          .build();
    }
    return CategoryDocument.builder().name(name).isSystem(true).build();
  }
}
