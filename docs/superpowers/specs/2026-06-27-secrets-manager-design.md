# Secrets Manager Integration — Spring Cloud AWS Migration Design

**Date:** 2026-06-27
**Status:** Approved
**Supersedes:** `2026-06-26-secrets-manager-design.md`

## Summary

Replace the custom `SecretsManagerEnvironmentPostProcessor` (107 lines of JSON parsing, AWS client
construction, and SPI boilerplate) with Spring Cloud AWS 4.0.0-RC1, which provides first-class
Spring Boot 4 support via `spring.config.import=aws-secretsmanager:...`. The custom class and its
`spring.factories` registration are deleted entirely.

Two secrets remain separate (one per concern) for independent IAM policy scoping — the service
can be granted access to MongoDB credentials or the JWT signing key independently.

## Secrets in scope

| Secret name | JSON payload | Spring property |
|---|---|---|
| `/mithril-vault/mongodb` | `{"spring.mongodb.uri": "mongodb://..."}` | `spring.mongodb.uri` |
| `/mithril-vault/jwt` | `{"app.jwt.secret-key": "..."}` | `app.jwt.secret-key` |

JSON keys are renamed to match Spring property names so Spring Cloud AWS can bind them directly
without any mapping logic.

## Why Spring Cloud AWS

Spring Cloud AWS 4.0.0-RC1 supports Spring Boot 4.0 and handles `spring.config.import` with the
`aws-secretsmanager:` prefix natively. At boot time it fetches each named secret, flattens the
JSON into Spring properties, and adds them to the `Environment` — the same effect as the custom
post-processor, with zero custom code.

`optional:` prefix prevents startup failure when a secret is absent (e.g., IT tests where secrets
are disabled and properties are provided inline).

## IAM design

| IAM policy | Secret(s) | Granted to |
|---|---|---|
| `mithril-vault-db-read` | `/mithril-vault/mongodb` | app role, DBA rotation script |
| `mithril-vault-jwt-read` | `/mithril-vault/jwt` | app role only |

Keeping secrets separate means a future auth-only service could get `mithril-vault-jwt-read`
without also receiving DB credentials.

## Configuration

### `application.yaml` (shared)
```yaml
spring:
  config:
    import:
      - "optional:aws-secretsmanager:/mithril-vault/mongodb"
      - "optional:aws-secretsmanager:/mithril-vault/jwt"
```

### `application-local.yaml`
```yaml
spring:
  cloud:
    aws:
      region:
        static: us-east-1
      credentials:
        access-key: test
        secret-key: test
      secretsmanager:
        endpoint: http://localhost:4566
```

### `application-prod.yaml`
```yaml
spring:
  cloud:
    aws:
      region:
        static: us-east-1
```
Credentials are supplied by the IAM instance/task role via `DefaultCredentialsProvider` —
nothing extra to configure.

### `application-it.yaml`
```yaml
spring:
  cloud:
    aws:
      secretsmanager:
        enabled: false
  config:
    import: ""
app:
  jwt:
    secret-key: integration-test-secret-not-for-production-use!!
```
MongoDB URI is already provided by Testcontainers `@DynamicPropertySource`.

## Dependency change

```toml
# libs.versions.toml — replaces aws-sdk = "2.31.9"
spring-cloud-aws = "4.0.0-RC1"

[libraries]
# replaces aws-secretsmanager
spring-cloud-aws-secrets = { module = "io.awspring.cloud:spring-cloud-aws-starter-secrets-manager", version.ref = "spring-cloud-aws" }
```

Spring milestone repo added to `settings.gradle` `dependencyResolutionManagement.repositories`.

## Files deleted

- `SecretsManagerEnvironmentPostProcessor.java`
- `META-INF/spring.factories`
- `SecretsManagerEnvironmentPostProcessorTest.java`

## LocalStack seed script

`localstack/init/01-seed-secrets.sh` — JSON keys renamed to match Spring property names:

```bash
awslocal secretsmanager create-secret \
  --name /mithril-vault/mongodb \
  --secret-string '{"spring.mongodb.uri":"mongodb://..."}'

awslocal secretsmanager create-secret \
  --name /mithril-vault/jwt \
  --secret-string '{"app.jwt.secret-key":"local-dev-jwt-secret-at-least-32-chars!!"}'
```
