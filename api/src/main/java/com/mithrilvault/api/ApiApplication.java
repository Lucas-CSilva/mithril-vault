package com.mithrilvault.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiApplication {

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    SpringApplication.run(ApiApplication.class, args);
  }
}
