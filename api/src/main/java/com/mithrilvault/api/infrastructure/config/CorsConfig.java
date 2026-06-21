package com.mithrilvault.api.infrastructure.config;

import com.mithrilvault.api.domain.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebFluxConfigurer {

  private final AppProperties appProperties;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = appProperties.cors().allowedOrigins().toArray(String[]::new);

    registry
        .addMapping("/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
