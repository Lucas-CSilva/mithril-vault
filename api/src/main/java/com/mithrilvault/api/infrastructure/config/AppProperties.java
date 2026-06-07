package com.mithrilvault.api.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Security security) {

  public record Cors(List<String> allowedOrigins) {}

  public record Security(List<String> publicPaths) {}
}
