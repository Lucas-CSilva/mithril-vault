# Secrets Manager Integration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate LocalStack-backed AWS Secrets Manager to supply `spring.mongodb.uri` and `app.jwt.secret-key` at Spring Boot startup via a custom `EnvironmentPostProcessor` using the AWS SDK v2.

**Architecture:** A `SecretsManagerEnvironmentPostProcessor` registered via `spring.factories` runs during `ApplicationEnvironmentPreparedEvent` — after profile-specific config files are loaded but before any bean is built. It fetches two JSON secrets from Secrets Manager, parses each JSON object, and injects the extracted values as a highest-priority `MapPropertySource`. LocalStack is added to Docker Compose and self-seeds secrets on first boot via a shell script.

**Tech Stack:** AWS SDK v2 `secretsmanager`, LocalStack (Docker), Spring Boot 4 `EnvironmentPostProcessor`, Jackson (transitive from AWS SDK), JUnit 5 + Mockito for unit testing.

## Global Constraints

- Spring Boot **4.0.6** — do not change this version.
- Money is `Long` centavos everywhere — not relevant here but noted.
- No AWS CLI required on the host — seeding uses `awslocal` inside the LocalStack container.
- Dependency locking is active (`gradle.lockfile`) — must regenerate after adding dependencies.
- Google Java Format enforced by Spotless — run `./gradlew spotlessApply` before committing Java.
- No comments in code — names describe intent.
- Conventional Commits (`feat:`, `chore:`, etc.).

---

## File Map

| File | Action | Purpose |
|---|---|---|
| `api/gradle/libs.versions.toml` | Modify | Add `aws-sdk` version + `aws-secretsmanager` library alias |
| `api/build.gradle` | Modify | Add `implementation libs.aws.secretsmanager` |
| `api/gradle.lockfile` | Regenerate | Updated by `--write-locks` after adding dependency |
| `localstack/init/01-seed-secrets.sh` | Create | Seeds the two secrets into LocalStack on first boot |
| `docker-compose.yml` | Modify | Add `localstack` service + volume for init script |
| `api/src/main/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessor.java` | Create | Fetches secrets and injects them into the Spring `Environment` |
| `api/src/main/resources/META-INF/spring.factories` | Create | Registers the post-processor with Spring Boot |
| `api/src/test/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessorTest.java` | Create | Unit tests for the post-processor (no Spring context) |
| `api/src/main/resources/application.yaml` | Modify | Remove hardcoded `spring.mongodb.uri` and `${JWT_SECRET_KEY}` fallback |
| `api/src/main/resources/application-local.yaml` | Modify | Add `aws.secretsmanager.endpoint` + `region` |
| `api/src/main/resources/application-prod.yaml` | Modify | Remove `MONGODB_URI` env var ref; add `aws.secretsmanager.region` |
| `api/src/test/resources/application-it.yaml` | Modify | Disable secrets manager; add `app.jwt.secret-key` for tests |

---

## Task 1: Add AWS SDK v2 Secrets Manager dependency

**Files:**
- Modify: `api/gradle/libs.versions.toml`
- Modify: `api/build.gradle`
- Regenerate: `api/gradle.lockfile`

**Interfaces:**
- Produces: `libs.aws.secretsmanager` alias available in `build.gradle`; classes `software.amazon.awssdk.services.secretsmanager.*` on the classpath

- [ ] **Step 1: Add version and library to the version catalog**

In `api/gradle/libs.versions.toml`, add to the `[versions]` block (after `archunit`):

```toml
aws-sdk = "2.31.9"
```

And add to the `[libraries]` block (after `archunit-junit5`):

```toml
aws-secretsmanager = { module = "software.amazon.awssdk:secretsmanager", version.ref = "aws-sdk" }
```

- [ ] **Step 2: Add the dependency to `build.gradle`**

In `api/build.gradle`, inside the `// ── Production ────────────────────────────` block, add after `libs.logstash.logback.encoder`:

```groovy
implementation libs.aws.secretsmanager
```

- [ ] **Step 3: Regenerate the dependency lockfile**

```bash
cd api && ./gradlew dependencies --write-locks
```

Expected: task completes successfully, `gradle.lockfile` is updated with new `software.amazon.awssdk:*` entries.

- [ ] **Step 4: Verify the build compiles**

```bash
cd api && ./gradlew classes
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add api/gradle/libs.versions.toml api/build.gradle api/gradle.lockfile
git commit -m "chore: add AWS SDK v2 secretsmanager dependency"
```

---

## Task 2: Add LocalStack to Docker Compose

**Files:**
- Create: `localstack/init/01-seed-secrets.sh`
- Modify: `docker-compose.yml`

**Interfaces:**
- Produces: LocalStack available at `http://localhost:4566`; secrets `/mithril-vault/mongodb` and `/mithril-vault/jwt` seeded on startup

- [ ] **Step 1: Create the init script**

Create `localstack/init/01-seed-secrets.sh`:

```bash
#!/bin/bash
set -e

awslocal secretsmanager create-secret \
  --name /mithril-vault/mongodb \
  --secret-string '{"uri":"mongodb://root:root@localhost:27017/mithril_vault?authSource=admin&replicaSet=rs0"}'

awslocal secretsmanager create-secret \
  --name /mithril-vault/jwt \
  --secret-string '{"secretKey":"local-dev-jwt-secret-at-least-32-chars!!"}'
```

Make it executable:

```bash
chmod +x localstack/init/01-seed-secrets.sh
```

- [ ] **Step 2: Add LocalStack service to `docker-compose.yml`**

Add the following service after the `mongo-init` service, and add `localstack_data` to the `volumes` block:

```yaml
  localstack:
    image: localstack/localstack:latest
    container_name: mithril_localstack
    ports:
      - "4566:4566"
    environment:
      - SERVICES=secretsmanager
      - AWS_DEFAULT_REGION=us-east-1
    volumes:
      - localstack_data:/var/lib/localstack
      - ./localstack/init:/etc/localstack/init/ready.d
    healthcheck:
      test: ["CMD", "awslocal", "secretsmanager", "list-secrets"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 15s
```

And in the `volumes` block:

```yaml
  localstack_data:
    driver: local
```

- [ ] **Step 3: Start LocalStack and verify secrets are seeded**

```bash
docker compose up -d localstack
docker compose logs localstack --follow
```

Wait until you see `Execution of "01-seed-secrets.sh" was successful`, then:

```bash
docker exec mithril_localstack awslocal secretsmanager list-secrets
```

Expected: JSON listing both `/mithril-vault/mongodb` and `/mithril-vault/jwt`.

Verify the secret values:

```bash
docker exec mithril_localstack awslocal secretsmanager get-secret-value --secret-id /mithril-vault/mongodb
docker exec mithril_localstack awslocal secretsmanager get-secret-value --secret-id /mithril-vault/jwt
```

Expected: each response contains a `SecretString` with the JSON payload.

- [ ] **Step 4: Commit**

```bash
git add localstack/ docker-compose.yml
git commit -m "chore: add LocalStack to Docker Compose and seed Secrets Manager"
```

---

## Task 3: Implement `SecretsManagerEnvironmentPostProcessor`

**Files:**
- Create: `api/src/main/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessor.java`
- Create: `api/src/main/resources/META-INF/spring.factories`
- Create: `api/src/test/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessorTest.java`

**Interfaces:**
- Consumes: `aws.secretsmanager.enabled` (Boolean, default `true`), `aws.secretsmanager.endpoint` (String, optional), `aws.secretsmanager.region` (String, default `us-east-1`) from the `Environment`
- Produces: `spring.mongodb.uri` and `app.jwt.secret-key` injected as `MapPropertySource` named `"aws-secrets-manager"` at the front of the property source list

- [ ] **Step 1: Write the failing unit tests**

Create `api/src/test/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessorTest.java`:

```java
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
            argThat((GetSecretValueRequest r) -> r.secretId().equals(secretName))))
        .thenReturn(GetSecretValueResponse.builder().secretString(secretString).build());
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd api && ./gradlew test --tests "com.mithrilvault.api.infrastructure.config.SecretsManagerEnvironmentPostProcessorTest"
```

Expected: compilation failure — `SecretsManagerEnvironmentPostProcessor` does not exist yet.

- [ ] **Step 3: Implement the post-processor**

Create `api/src/main/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessor.java`:

```java
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

  private final Function<ConfigurableEnvironment, SecretsManagerClient> clientFactory;
  private final ObjectMapper objectMapper = new ObjectMapper();

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

    SecretsManagerClient client = clientFactory.apply(environment);
    Map<String, Object> properties = new HashMap<>();

    for (SecretMapping mapping : MAPPINGS) {
      String secretString = fetchSecret(client, mapping.secretName());
      properties.put(mapping.springProperty(), extractJsonField(secretString, mapping.jsonKey()));
    }

    environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
  }

  private SecretsManagerClient buildClient(ConfigurableEnvironment environment) {
    String endpoint = environment.getProperty(ENDPOINT_PROPERTY);
    String region = environment.getProperty(REGION_PROPERTY, "us-east-1");

    SecretsManagerClientBuilder builder =
        SecretsManagerClient.builder().region(Region.of(region));

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
      Map<String, String> parsed = objectMapper.readValue(jsonSecret, new TypeReference<>() {});
      String value = parsed.get(fieldName);
      if (value == null) {
        throw new IllegalStateException(
            "Field '" + fieldName + "' not found in secret JSON");
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
```

- [ ] **Step 4: Register the post-processor**

Create `api/src/main/resources/META-INF/spring.factories`:

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
  com.mithrilvault.api.infrastructure.config.SecretsManagerEnvironmentPostProcessor
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd api && ./gradlew spotlessApply && ./gradlew test --tests "com.mithrilvault.api.infrastructure.config.SecretsManagerEnvironmentPostProcessorTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests passing.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessor.java \
        api/src/main/resources/META-INF/spring.factories \
        api/src/test/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessorTest.java
git commit -m "feat: add SecretsManagerEnvironmentPostProcessor to resolve secrets at startup"
```

---

## Task 4: Update configuration files

**Files:**
- Modify: `api/src/main/resources/application.yaml`
- Modify: `api/src/main/resources/application-local.yaml`
- Modify: `api/src/main/resources/application-prod.yaml`
- Modify: `api/src/test/resources/application-it.yaml`

**Interfaces:**
- Consumes: `spring.mongodb.uri` and `app.jwt.secret-key` supplied by the post-processor (or `@DynamicPropertySource` in ITs)
- Produces: clean config files with no hardcoded credentials

- [ ] **Step 1: Remove hardcoded credentials from `application.yaml`**

In `api/src/main/resources/application.yaml`:

Remove the entire `uri:` line under `spring.mongodb`:

```yaml
spring:
  webflux:
    base-path: /mithril-vault
    codecs:
      max-in-memory-size: 16MB

  mongodb:
      connection-pool:
        min-size: 10
        max-size: 100
        max-wait-time: 5000ms
        max-connection-life-time: 30m
        max-connection-idle-time: 10m
```

And change the `app.jwt.secret-key` line to remove the env var fallback — the key is now omitted entirely from this file (the post-processor provides it). Remove this line:

```yaml
    secret-key: ${JWT_SECRET_KEY:change-me-in-production-at-least-32-chars!!}
```

The `app.jwt` block becomes:

```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://127.0.0.1:3000
  security:
    public-paths:
      - /actuator/health
      - /actuator/info
      - /swagger-ui.html
      - /swagger-ui/**
      - /v3/api-docs/**
      - /webjars/**
      - /register
      - /login
      - /refresh
      - /logout
  jwt:
    access-token-ttl-seconds: 900
    refresh-token-ttl-seconds: 2592000
```

- [ ] **Step 2: Add LocalStack config to `application-local.yaml`**

Append to `api/src/main/resources/application-local.yaml`:

```yaml
aws:
  secretsmanager:
    endpoint: http://localhost:4566
    region: us-east-1
```

- [ ] **Step 3: Update `application-prod.yaml`**

Replace `${MONGODB_URI}` with nothing (the URI comes from Secrets Manager). The `spring.mongodb` block becomes connection-pool only. Add the AWS region. Final file:

```yaml
spring:
  mongodb:
      connection-pool:
        min-size: 20
        max-size: 200
        max-wait-time: 3000ms
        max-connection-life-time: 1h
        max-connection-idle-time: 15m

logging:
  level:
    root: WARN
    org.springframework.web: WARN
    org.springframework.data.mongodb: WARN
    org.mongodb.driver: WARN
    com.mithrilvault.api: INFO

management:
  endpoint:
    health:
      show-details: never
  endpoints:
    web:
      exposure:
        include: health

server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css
  http2:
    enabled: true

app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}

aws:
  secretsmanager:
    region: us-east-1
```

- [ ] **Step 4: Update `application-it.yaml` to disable secrets manager and supply a JWT secret**

Replace the content of `api/src/test/resources/application-it.yaml` with:

```yaml
logging:
  level:
    root: WARN
    com.mithrilvault.api: INFO

aws:
  secretsmanager:
    enabled: false

app:
  jwt:
    secret-key: integration-test-secret-not-for-production-use!!
```

Note: `spring.mongodb.uri` is supplied by `AbstractIntegrationTest.@DynamicPropertySource` (Testcontainers), so it is not needed here.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/application.yaml \
        api/src/main/resources/application-local.yaml \
        api/src/main/resources/application-prod.yaml \
        api/src/test/resources/application-it.yaml
git commit -m "chore: remove hardcoded credentials and wire config to Secrets Manager"
```

---

## Task 5: End-to-end verification

**Files:** none created — verification only.

- [ ] **Step 1: Run the full unit test suite**

```bash
cd api && ./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all unit tests pass.

- [ ] **Step 2: Run integration tests**

```bash
cd api && ./gradlew integrationTest
```

Expected: `BUILD SUCCESSFUL`, all `*IT` tests pass. The `aws.secretsmanager.enabled: false` flag prevents the post-processor from running; Testcontainers supplies the MongoDB URI and the IT yaml supplies the JWT secret.

- [ ] **Step 3: Start the full local stack**

```bash
docker compose up -d
```

Wait for MongoDB and LocalStack to be healthy:

```bash
docker compose ps
```

Expected: `mongodb`, `mongo-init`, and `localstack` all show as `healthy` or `exited (0)`.

- [ ] **Step 4: Verify LocalStack secrets are seeded**

```bash
docker exec mithril_localstack awslocal secretsmanager get-secret-value --secret-id /mithril-vault/mongodb
```

Expected: `SecretString` contains `{"uri":"mongodb://root:root@localhost:27017/mithril_vault?authSource=admin&replicaSet=rs0"}`.

- [ ] **Step 5: Start the API with the local profile and verify it resolves secrets**

```bash
cd api && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Watch the startup log. You should NOT see any `spring.mongodb.uri` not-set error. The app should start and reach `Started MithrilVaultApplication`.

- [ ] **Step 6: Smoke-test the health endpoint**

```bash
curl -s http://localhost:8080/mithril-vault/actuator/health | python3 -m json.tool
```

Expected:
```json
{
    "status": "UP",
    "components": {
        "mongo": { "status": "UP" },
        ...
    }
}
```

`mongo: UP` confirms the URI resolved correctly from Secrets Manager.

- [ ] **Step 7: Commit verification note (optional)**

If any minor fixes were needed during verification, commit them now:

```bash
git add -p
git commit -m "fix: <describe what needed fixing>"
```
