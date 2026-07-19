package com.mithrilvault.api.infrastructure.config;

import com.mongodb.reactivestreams.client.MongoClient;
import io.mongock.driver.api.driver.ConnectionDriver;
import io.mongock.driver.mongodb.reactive.driver.MongoReactiveDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;

@Configuration
public class MongockConfig {

  @Bean
  public ConnectionDriver connectionDriver(
      MongoClient mongoClient, ReactiveMongoDatabaseFactory mongoDatabaseFactory) {
    String databaseName = mongoDatabaseFactory.getMongoDatabase().block().getName();
    return MongoReactiveDriver.withDefaultLock(mongoClient, databaseName);
  }
}
