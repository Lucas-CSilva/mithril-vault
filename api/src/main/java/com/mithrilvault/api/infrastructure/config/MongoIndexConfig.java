package com.mithrilvault.api.infrastructure.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

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

      mongoTemplate
          .indexOps("users")
          .createIndex(emailIndex)
          .then(mongoTemplate.indexOps("refresh_tokens").createIndex(tokenHashIndex))
          .then(mongoTemplate.indexOps("refresh_tokens").createIndex(userIdIndex))
          .then(mongoTemplate.indexOps("refresh_tokens").createIndex(expiresAtIndex))
          .subscribe();
    };
  }
}
