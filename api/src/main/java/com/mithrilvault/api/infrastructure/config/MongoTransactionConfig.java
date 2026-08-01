package com.mithrilvault.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

  @Bean
  ReactiveMongoTransactionManager transactionManager(ReactiveMongoDatabaseFactory factory) {
    return new ReactiveMongoTransactionManager(factory);
  }

  @Bean
  public TransactionalOperator transactionalOperator(
      ReactiveMongoTransactionManager transactionManager) {
    return TransactionalOperator.create(transactionManager);
  }
}
