package com.mithrilvault.api.infrastructure.config;

import com.mithrilvault.api.domain.config.AppProperties;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  @Bean
  Clock clock(AppProperties appProperties) {
    return Clock.system(appProperties.zone());
  }
}
