package com.mithrilvault.api.domain.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Cors cors,
    Security security,
    Jwt jwt,
    ProjectionLeaderConfig leader,
    SchedulerConfig scheduler) {

  public record Cors(List<String> allowedOrigins) {}

  public record Security(List<String> publicPaths) {}

  public record Jwt(String secretKey, Long accessTokenTtlSeconds, Long refreshTokenTtlSeconds) {}

  public record ProjectionLeaderConfig(Duration ttl, Integer maxRetries, Duration retryBackoff) {}

  public record SchedulerConfig(
      SchedulerJobConfig balanceSnapshot,
      BalanceReconciliationJobConfig balanceReconciliation,
      ZoneId zone) {}

  public record SchedulerJobConfig(CronExpression cron, Integer concurrency) {}

  public record BalanceReconciliationJobConfig(
      CronExpression cron,
      Integer concurrency,
      Integer conflictRetryMaxAttempts,
      Duration conflictRetryBackoff) {}
}
