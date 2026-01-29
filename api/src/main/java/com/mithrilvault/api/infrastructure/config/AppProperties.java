package com.mithrilvault.api.infrastructure.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private Cors cors;

  @Getter
  @Setter
  public static class Cors {
    private List<String> allowedOrigins;
  }

  public String[] getAllowedOrigins() {
    return this.cors.getAllowedOrigins().toArray(String[]::new);
  }
}
