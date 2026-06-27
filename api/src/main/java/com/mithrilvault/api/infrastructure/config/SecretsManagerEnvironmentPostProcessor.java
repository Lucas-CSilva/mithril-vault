package com.mithrilvault.api.infrastructure.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SecretsManagerEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String ENABLED_PROPERTY = "aws.secretsmanager.enabled";
  private static final String ENDPOINT_PROPERTY = "aws.secretsmanager.endpoint";
  private static final String REGION_PROPERTY = "aws.secretsmanager.region";
  private static final String PROPERTY_SOURCE_NAME = "aws-secrets-manager";

  private static final List<SecretMapping> MAPPINGS =
      List.of(
          new SecretMapping("/mithril-vault/mongodb", "uri", "spring.mongodb.uri"),
          new SecretMapping("/mithril-vault/jwt", "secretKey", "app.jwt.secret-key"));

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final Function<ConfigurableEnvironment, SecretsManagerClient> clientFactory;

  public SecretsManagerEnvironmentPostProcessor() {
    this.clientFactory = this::buildClient;
  }

  SecretsManagerEnvironmentPostProcessor(SecretsManagerClient client) {
    this.clientFactory = env -> client;
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (!environment.getProperty(ENABLED_PROPERTY, Boolean.class, true)) {
      return;
    }

    try (SecretsManagerClient client = clientFactory.apply(environment)) {
      Map<String, Object> properties = new HashMap<>();

      for (SecretMapping mapping : MAPPINGS) {
        String secretString = fetchSecret(client, mapping.secretName());
        properties.put(mapping.springProperty(), extractJsonField(secretString, mapping.jsonKey()));
      }

      environment
          .getPropertySources()
          .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }
  }

  private SecretsManagerClient buildClient(ConfigurableEnvironment environment) {
    String endpoint = environment.getProperty(ENDPOINT_PROPERTY);
    String region = environment.getProperty(REGION_PROPERTY, "us-east-1");

    SecretsManagerClientBuilder builder = SecretsManagerClient.builder().region(Region.of(region));

    if (endpoint != null) {
      builder
          .endpointOverride(URI.create(endpoint))
          .credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }

    return builder.build();
  }

  private String fetchSecret(SecretsManagerClient client, String secretName) {
    return client
        .getSecretValue(GetSecretValueRequest.builder().secretId(secretName).build())
        .secretString();
  }

  private String extractJsonField(String jsonSecret, String fieldName) {
    try {
      Map<String, String> parsed = OBJECT_MAPPER.readValue(jsonSecret, new TypeReference<>() {});
      String value = parsed.get(fieldName);
      if (value == null) {
        throw new IllegalStateException("Field '" + fieldName + "' not found in secret JSON");
      }
      return value;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse secret JSON: " + e.getMessage(), e);
    }
  }

  private record SecretMapping(String secretName, String jsonKey, String springProperty) {}
}
