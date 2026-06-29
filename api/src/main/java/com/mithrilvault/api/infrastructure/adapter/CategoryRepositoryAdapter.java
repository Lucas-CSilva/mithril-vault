package com.mithrilvault.api.infrastructure.adapter;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.domain.port.CategoryReadRepository;
import com.mithrilvault.api.domain.port.CategoryRepository;
import com.mithrilvault.api.infrastructure.mapper.CategoryMapper;
import com.mithrilvault.api.infrastructure.persistence.CategoryMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import org.springframework.dao.DuplicateKeyException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository, CategoryReadRepository {
  private final CategoryMongoRepository mongoRepository;
  private final CategoryMapper categoryMapper;
  private final ReactiveMongoTemplate reactiveMongoTemplate;

  @Override
  public Flux<Category> findAllVisibleToOwner(String ownerId) {
    return reactiveMongoTemplate
        .find(
            Query.query(
                new Criteria()
                    .orOperator(
                        Criteria.where(CategoryDocument.Fields.ownerId).is(ownerId),
                        Criteria.where(CategoryDocument.Fields.isSystem).is(true))),
            CategoryDocument.class)
        .map(categoryMapper::toDomain);
  }

  @Override
  public Mono<Category> findVisibleById(String id, String ownerId) {
    return reactiveMongoTemplate
        .findOne(
            Query.query(
                new Criteria()
                    .andOperator(
                        Criteria.where("_id").is(id),
                        new Criteria()
                            .orOperator(
                                Criteria.where(CategoryDocument.Fields.ownerId).is(ownerId),
                                Criteria.where(CategoryDocument.Fields.isSystem).is(true)))),
            CategoryDocument.class)
        .map(categoryMapper::toDomain);
  }

  @Override
  public Mono<Category> findById(String id) {
    return mongoRepository.findById(id).map(categoryMapper::toDomain);
  }

  @Override
  public Flux<Category> findChildrenByParentId(String parentId) {
    return reactiveMongoTemplate
        .find(
            Query.query(Criteria.where(CategoryDocument.Fields.parentId).is(parentId)),
            CategoryDocument.class)
        .map(categoryMapper::toDomain);
  }

  @Override
  public Mono<Category> save(Category category) {
    return mongoRepository
        .save(categoryMapper.toDocument(category))
        .onErrorMap(
            DuplicateKeyException.class,
            ex -> new ConflictException("Category name already exists"))
        .map(categoryMapper::toDomain);
  }

  @Override
  @Transactional
  public Mono<Void> deleteWithReassignment(
      String categoryId, List<String> childIds, String outrosId) {

    return reactiveMongoTemplate
        .updateMulti(
            Query.query(Criteria.where("_id").in(childIds)),
            Update.update(CategoryDocument.Fields.parentId, outrosId),
            CategoryDocument.class)
        .then(mongoRepository.deleteById(categoryId));
  }
}
