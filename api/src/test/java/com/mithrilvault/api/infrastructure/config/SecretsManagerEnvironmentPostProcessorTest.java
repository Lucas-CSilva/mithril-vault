package com.mithrilvault.api.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class SecretsManagerEnvironmentPostProcessorTest {

  private final SecretsManagerClient client = mock(SecretsManagerClient.class);
  private final SpringApplication app = mock(SpringApplication.class);

  @Test
  void skips_when_disabled() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("aws.secretsmanager.enabled", "false");

    new SecretsManagerEnvironmentPostProcessor(client).postProcessEnvironment(env, app);

    verifyNoInteractions(client);
  }

  @Test
  void injects_mongodb_uri_and_jwt_secret_as_highest_priority_source() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("some.other.property", "existing-value");

    stubSecret("/mithril-vault/mongodb", "{\"uri\":\"mongodb://host:27017/db\"}");
    stubSecret("/mithril-vault/jwt", "{\"secretKey\":\"jwt-secret-32-chars-minimum!!!!\"}");

    new SecretsManagerEnvironmentPostProcessor(client).postProcessEnvironment(env, app);

    assertThat(env.getProperty("spring.mongodb.uri")).isEqualTo("mongodb://host:27017/db");
    assertThat(env.getProperty("app.jwt.secret-key")).isEqualTo("jwt-secret-32-chars-minimum!!!!");
    assertThat(env.getPropertySources().iterator().next().getName())
        .isEqualTo("aws-secrets-manager");
  }

  @Test
  void aws_secrets_override_existing_environment_properties() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("spring.mongodb.uri", "mongodb://old-value");

    stubSecret("/mithril-vault/mongodb", "{\"uri\":\"mongodb://new-value\"}");
    stubSecret("/mithril-vault/jwt", "{\"secretKey\":\"jwt-secret-32-chars-minimum!!!!\"}");

    new SecretsManagerEnvironmentPostProcessor(client).postProcessEnvironment(env, app);

    assertThat(env.getProperty("spring.mongodb.uri")).isEqualTo("mongodb://new-value");
  }

  private void stubSecret(String secretName, String secretString) {
    when(client.getSecretValue(
            argThat((GetSecretValueRequest r) -> r != null && r.secretId().equals(secretName))))
        .thenReturn(GetSecretValueResponse.builder().secretString(secretString).build());
  }
}
