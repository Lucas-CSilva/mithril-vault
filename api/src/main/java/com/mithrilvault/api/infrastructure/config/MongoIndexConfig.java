package com.mithrilvault.api.infrastructure.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Collation;

@Configuration
@EnableReactiveMongoAuditing
public class MongoIndexConfig {

  @Bean
  ApplicationRunner createMongoIndexes(ReactiveMongoTemplate mongoTemplate) {
    return args -> {
      Index emailIndex = new Index().on("email", Sort.Direction.ASC).unique();

      Index tokenHashIndex = new Index().on("tokenHash", Sort.Direction.ASC).unique().sparse();
      Index userIdIndex = new Index().on("userId", Sort.Direction.ASC);
      Index expiresAtIndex = new Index().on("expiresAt", Sort.Direction.ASC).expire(0);

      Index categoryOwnerIndex = new Index().on("ownerId", Sort.Direction.ASC).sparse();
      Index categorySystemIndex = new Index().on("isSystem", Sort.Direction.ASC);
      Index categoryNameIndex =
          new Index()
              .on("ownerId", Sort.Direction.ASC)
              .on("name", Sort.Direction.ASC)
              .unique()
              .sparse();

      Index accountOwnerIndex = new Index().on("ownerId", Sort.Direction.ASC);
      Index accountNameIndex =
          new Index()
              .on("ownerId", Sort.Direction.ASC)
              .on("name", Sort.Direction.ASC)
              .unique()
              .collation(Collation.of("pt").strength(2));

      mongoTemplate
          .indexOps("users")
          .createIndex(emailIndex)
          .then(mongoTemplate.indexOps("refresh_tokens").createIndex(tokenHashIndex))
          .then(mongoTemplate.indexOps("refresh_tokens").createIndex(userIdIndex))
          .then(mongoTemplate.indexOps("refresh_tokens").createIndex(expiresAtIndex))
          .then(mongoTemplate.indexOps("categories").createIndex(categoryOwnerIndex))
          .then(mongoTemplate.indexOps("categories").createIndex(categorySystemIndex))
          .then(mongoTemplate.indexOps("categories").createIndex(categoryNameIndex))
          .then(mongoTemplate.indexOps("accounts").createIndex(accountOwnerIndex))
          .then(mongoTemplate.indexOps("accounts").createIndex(accountNameIndex))
          .subscribe();
    };
  }
}
