package com.mithrilvault.api.infrastructure.config;

import com.mongodb.ConnectionString;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories
public class MongoConfig extends AbstractReactiveMongoConfiguration {

  private final String mongoUri;

  public MongoConfig(@Value("${spring.data.mongodb.uri}") String mongoUri) {
    this.mongoUri = mongoUri;
  }

  @Override
  protected String getDatabaseName() {
    ConnectionString connectionString = new ConnectionString(mongoUri);
    return connectionString.getDatabase();
  }

  @Override
  @Bean
  public MongoClient reactiveMongoClient() {
    return MongoClients.create(mongoUri);
  }

  @Bean
  public ReactiveMongoTemplate reactiveMongoTemplate() {
    return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
  }
}
