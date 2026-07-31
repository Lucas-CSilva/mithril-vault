package com.mithrilvault.api.domain.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Security security, Jwt jwt, ProjectionLeaderConfig leader) {

  public record Cors(List<String> allowedOrigins) {}

  public record Security(List<String> publicPaths) {}

  public record Jwt(String secretKey, Long accessTokenTtlSeconds, Long refreshTokenTtlSeconds) {}

  public record ProjectionLeaderConfig(Duration ttl, Integer maxRetries, Duration retryBackoff) {}
}
