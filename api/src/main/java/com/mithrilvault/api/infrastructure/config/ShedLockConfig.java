package com.mithrilvault.api.infrastructure.config;

import java.util.Objects;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.reactivestreams.ReactiveStreamsMongoLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ShedLockConfig {

  @Bean
  public LockProvider lockProvider(ReactiveMongoTemplate mongoTemplate) {
    return new ReactiveStreamsMongoLockProvider(
        Objects.requireNonNull(mongoTemplate.getMongoDatabase().block()));
  }
}
