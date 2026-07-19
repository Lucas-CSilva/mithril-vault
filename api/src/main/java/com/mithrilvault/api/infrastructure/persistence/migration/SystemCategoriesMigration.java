package com.mithrilvault.api.infrastructure.persistence.migration;

import com.mithrilvault.api.domain.config.SystemCategoryIds;
import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@ChangeUnit(id = "system-categories-migration", order = "001", author = "admin")
public class SystemCategoriesMigration {

  private record CategorySeed(String name, String icon, String color) {}

  private static final List<CategorySeed> TOP_LEVEL_CATEGORIES =
      List.of(
          new CategorySeed("Alimentação", "cart", "#B0795F"),
          new CategorySeed("Moradia", "home", "#3C5070"),
          new CategorySeed("Transporte", "car", "#5E7A96"),
          new CategorySeed("Saúde", "heart", "#8E3A4B"),
          new CategorySeed("Educação", "book", "#3E6B82"),
          new CategorySeed("Lazer", "sparkle", "#9A8AA3"),
          new CategorySeed("Vestuário", "shirt", "#A87E84"),
          new CategorySeed("Serviços & Assinaturas", "repeat", "#9E7A4E"),
          new CategorySeed("Investimentos", "trending", "#6E7E96"),
          new CategorySeed("Transferências", "swap", "#8A93A3"),
          new CategorySeed("Renda", "arrow-down-left", "#4E7C66"),
          new CategorySeed("Outros", "dots", "#9298A2"));

  private static final Map<String, String> SUBCATEGORY_ICON_OVERRIDES =
      Map.ofEntries(
          Map.entry("Supermercado", "cart"),
          Map.entry("Restaurante", "utensils"),
          Map.entry("Delivery", "bag"),
          Map.entry("Aluguel", "home"),
          Map.entry("Energia", "bolt"),
          Map.entry("Combustível", "fuel"),
          Map.entry("Aplicativo", "car"),
          Map.entry("Streaming", "play"));

  private static final Map<String, List<String>> SUBCATEGORIES_BY_PARENT =
      Map.ofEntries(
          Map.entry("Alimentação", List.of("Supermercado", "Delivery", "Restaurante", "Padaria")),
          Map.entry("Moradia", List.of("Aluguel", "Energia", "Água", "Internet", "Condomínio")),
          Map.entry(
              "Transporte",
              List.of("Combustível", "Aplicativo", "Transporte Público", "Manutenção")),
          Map.entry("Saúde", List.of("Farmácia", "Consulta", "Academia", "Plano de Saúde")),
          Map.entry("Educação", List.of("Cursos", "Material", "Mensalidade")),
          Map.entry("Lazer", List.of("Cinema", "Viagem", "Entretenimento", "Hobby")),
          Map.entry("Vestuário", List.of("Roupas", "Calçados", "Acessórios")),
          Map.entry("Serviços & Assinaturas", List.of("Streaming", "Software", "Telefone")),
          Map.entry("Investimentos", List.of("Renda Fixa", "Tesouro Direto", "Ações")),
          Map.entry(
              "Renda", List.of("Salário", "Freelance", "Transferência Recebida", "Dividendos")));

  private final ReactiveMongoTemplate mongoTemplate;
  private final SystemCategoryIds systemCategoryIds;

  @Execution
  public Mono<Void> changeSet() {
    return seedTopLevelCategories().then(seedSubcategories());
  }

  @RollbackExecution
  public Mono<Void> rollback() {
    Query query = Query.query(Criteria.where(CategoryDocument.Fields.isSystem).is(true));
    return mongoTemplate.remove(query, CategoryDocument.class).then();
  }

  private Mono<Void> seedTopLevelCategories() {
    return Flux.fromIterable(TOP_LEVEL_CATEGORIES).flatMap(this::seedTopLevel).then();
  }

  private Mono<Void> seedSubcategories() {
    Map<String, CategorySeed> topLevelByName =
        TOP_LEVEL_CATEGORIES.stream().collect(Collectors.toMap(CategorySeed::name, seed -> seed));

    return Flux.fromIterable(SUBCATEGORIES_BY_PARENT.entrySet())
        .flatMap(
            entry -> {
              CategorySeed parentSeed = topLevelByName.get(entry.getKey());
              return findSystemCategoryId(entry.getKey())
                  .flatMapMany(
                      parentId ->
                          Flux.fromIterable(entry.getValue())
                              .flatMap(name -> seedSubcategory(name, parentId, parentSeed)));
            })
        .then();
  }

  private Mono<CategoryDocument> seedTopLevel(CategorySeed seed) {
    Query query =
        Query.query(
            Criteria.where(CategoryDocument.Fields.isSystem)
                .is(true)
                .and(CategoryDocument.Fields.name)
                .is(seed.name()));

    return mongoTemplate
        .findOne(query, CategoryDocument.class)
        .switchIfEmpty(Mono.defer(() -> mongoTemplate.save(buildTopLevelDocument(seed))));
  }

  private Mono<CategoryDocument> seedSubcategory(
      String name, String parentId, CategorySeed parentSeed) {
    Query query =
        Query.query(
            Criteria.where(CategoryDocument.Fields.isSystem)
                .is(true)
                .and(CategoryDocument.Fields.name)
                .is(name)
                .and(CategoryDocument.Fields.parentId)
                .is(parentId));

    String icon = SUBCATEGORY_ICON_OVERRIDES.getOrDefault(name, parentSeed.icon());

    return mongoTemplate
        .findOne(query, CategoryDocument.class)
        .switchIfEmpty(
            Mono.defer(
                () ->
                    mongoTemplate.save(
                        CategoryDocument.builder()
                            .name(name)
                            .parentId(parentId)
                            .icon(icon)
                            .color(parentSeed.color())
                            .isSystem(true)
                            .build())));
  }

  private Mono<String> findSystemCategoryId(String name) {
    Query query =
        Query.query(
            Criteria.where(CategoryDocument.Fields.isSystem)
                .is(true)
                .and(CategoryDocument.Fields.name)
                .is(name));
    return mongoTemplate.findOne(query, CategoryDocument.class).map(CategoryDocument::getId);
  }

  private CategoryDocument buildTopLevelDocument(CategorySeed seed) {
    CategoryDocument.CategoryDocumentBuilder builder =
        CategoryDocument.builder()
            .name(seed.name())
            .icon(seed.icon())
            .color(seed.color())
            .isSystem(true);

    if ("Outros".equals(seed.name())) {
      builder.id(systemCategoryIds.getOutrosId());
    }

    return builder.build();
  }
}
