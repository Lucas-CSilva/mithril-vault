package com.mithrilvault.api.infrastructure.config;

import com.mithrilvault.api.infrastructure.persistence.document.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Collation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
@EnableReactiveMongoAuditing
public class MongoIndexConfig {

  @Bean
  ApplicationRunner createMongoIndexes(ReactiveMongoTemplate mongoTemplate) {
    return args ->
        createIndexes(
                mongoTemplate,
                UserDocument.class,
                new Index().on(UserDocument.Fields.email, Sort.Direction.ASC).unique())
            .then(
                createIndexes(
                    mongoTemplate,
                    RefreshTokenDocument.class,
                    new Index()
                        .on(RefreshTokenDocument.Fields.tokenHash, Sort.Direction.ASC)
                        .unique()
                        .sparse(),
                    new Index().on(RefreshTokenDocument.Fields.userId, Sort.Direction.ASC),
                    new Index()
                        .on(RefreshTokenDocument.Fields.expiresAt, Sort.Direction.ASC)
                        .expire(0)))
            .then(
                createIndexes(
                    mongoTemplate,
                    CategoryDocument.class,
                    new Index().on(CategoryDocument.Fields.ownerId, Sort.Direction.ASC).sparse(),
                    new Index().on(CategoryDocument.Fields.isSystem, Sort.Direction.ASC),
                    new Index()
                        .on(CategoryDocument.Fields.ownerId, Sort.Direction.ASC)
                        .on(CategoryDocument.Fields.name, Sort.Direction.ASC)
                        .unique()
                        .sparse()))
            .then(
                createIndexes(
                    mongoTemplate,
                    AccountDocument.class,
                    new Index().on(AccountDocument.Fields.ownerId, Sort.Direction.ASC),
                    new Index()
                        .on(AccountDocument.Fields.ownerId, Sort.Direction.ASC)
                        .on(AccountDocument.Fields.name, Sort.Direction.ASC)
                        .unique()
                        .collation(Collation.of("pt").strength(2))))
            .then(
                createIndexes(
                    mongoTemplate,
                    BalanceSnapshotDocument.class,
                    new Index().on(BalanceSnapshotDocument.Fields.ownerId, Sort.Direction.ASC),
                    new Index().on(BalanceSnapshotDocument.Fields.accountId, Sort.Direction.ASC),
                    new Index().on(BalanceSnapshotDocument.Fields.asOfDate, Sort.Direction.DESC)))
            .subscribe();
  }

  private Mono<Void> createIndexes(
      ReactiveMongoTemplate mongoTemplate, Class<?> documentClass, Index... indexes) {
    String collection = mongoTemplate.getCollectionName(documentClass);
    return Flux.fromArray(indexes)
        .concatMap(index -> mongoTemplate.indexOps(collection).createIndex(index))
        .then();
  }
}
