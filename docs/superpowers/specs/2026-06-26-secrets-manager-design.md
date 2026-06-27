# Secrets Manager Integration — Design

**Date:** 2026-06-26
**Status:** Approved

## Summary

Integrate AWS Secrets Manager (via LocalStack for local development) to store MongoDB credentials
and the JWT secret key. Spring Boot resolves secrets through a custom `EnvironmentPostProcessor`
using the AWS SDK v2 directly — no Spring Cloud AWS.

## Secrets in scope

| Secret name | JSON payload | Spring property |
|---|---|---|
| `/mithril-vault/mongodb` | `{"uri": "mongodb://..."}` | `spring.mongodb.uri` |
| `/mithril-vault/jwt` | `{"secretKey": "..."}` | `app.jwt.secret-key` |

Secrets are JSON objects rather than plain strings so fields can be added later without creating
new secrets or changing IAM policies.

## `SecretsManagerEnvironmentPostProcessor`

**Location:** `api/src/main/java/com/mithrilvault/api/infrastructure/config/SecretsManagerEnvironmentPostProcessor.java`
**Registration:** `api/src/main/resources/META-INF/spring.factories`

Spring Boot invokes `EnvironmentPostProcessor` implementations during
`ApplicationEnvironmentPreparedEvent` — after all config files (including profile-specific ones)
are loaded but before any bean is instantiated. This allows the processor to read profile
properties (e.g. the LocalStack endpoint) from the already-prepared `Environment` and to inject
secrets as a high-priority `PropertySource` that the rest of the app sees as plain properties.

### Logic flow

1. Read `aws.secretsmanager.enabled` — if `false`, return immediately.
2. Read `aws.secretsmanager.endpoint` and `aws.secretsmanager.region` from the `Environment`.
3. Build `SecretsManagerClient`:
   - Custom endpoint present → `StaticCredentialsProvider("test", "test")` + endpoint override (LocalStack).
   - No endpoint → `DefaultCredentialsProvider` (picks up IAM instance/task role in production).
4. For each `(secretName, jsonKey → springProperty)` mapping: call `GetSecretValue`, parse the
   JSON string, extract the value.
5. Add all resolved values as a `MapPropertySource` named `"aws-secrets-manager"` at the front
   of the `Environment`'s property source list (highest priority).

### Hardcoded secret mappings

```
/mithril-vault/mongodb  →  uri        →  spring.mongodb.uri
/mithril-vault/jwt      →  secretKey  →  app.jwt.secret-key
```

## Configuration file changes

### `application.yaml`
- Remove hardcoded `spring.mongodb.uri` value and `${JWT_SECRET_KEY:...}` fallback — the
  post-processor supplies both at startup.

### `application-local.yaml`
```yaml
aws:
  secretsmanager:
    endpoint: http://localhost:4566
    region: us-east-1
```

### `application-prod.yaml`
- Remove `MONGODB_URI` env var reference — replaced by Secrets Manager via IAM role.
- Add `aws.secretsmanager.region: <region>`.

### `application-it.yaml`
```yaml
aws:
  secretsmanager:
    enabled: false

app:
  jwt:
    secret-key: integration-test-secret-not-for-production-use!!
```
Testcontainers `@DynamicPropertySource` already overrides `spring.mongodb.uri`; the JWT secret
is set directly so tests remain self-contained without needing LocalStack.

## Docker Compose

New `localstack` service added to `docker-compose.yml`:

```yaml
localstack:
  image: localstack/localstack:latest
  ports:
    - "4566:4566"
  environment:
    - SERVICES=secretsmanager
    - AWS_DEFAULT_REGION=us-east-1
  volumes:
    - localstack_data:/var/lib/localstack
    - ./localstack/init:/etc/localstack/init/ready.d
```

Only `secretsmanager` is enabled — no need to boot the full LocalStack suite.

### Init script: `localstack/init/01-seed-secrets.sh`

Runs automatically inside the LocalStack container once it is healthy. Uses `awslocal` (AWS CLI
wrapper pre-installed in LocalStack), so no AWS CLI installation is required on the host.

```bash
#!/bin/bash
awslocal secretsmanager create-secret \
  --name /mithril-vault/mongodb \
  --secret-string '{"uri":"mongodb://root:root@localhost:27017/mithril_vault?authSource=admin&replicaSet=rs0"}'

awslocal secretsmanager create-secret \
  --name /mithril-vault/jwt \
  --secret-string '{"secretKey":"local-dev-jwt-secret-at-least-32-chars!!"}'
```

## Gradle dependency

```toml
# libs.versions.toml
[versions]
aws-sdk = "2.31.9"

[libraries]
aws-secretsmanager = { module = "software.amazon.awssdk:secretsmanager", version.ref = "aws-sdk" }
```

Only the `secretsmanager` module is added — no full SDK, no Spring Cloud AWS. The module brings
`sdk-core` and Jackson transitively.

## What does NOT change

- Domain, application, and persistence layers — untouched.
- Existing `@Value` / property keys — same names, now backed by Secrets Manager.
- Integration tests — continue using Testcontainers MongoDB; secrets manager is disabled via profile.

## Production auth

No credentials in config. The AWS SDK `DefaultCredentialsProvider` chain picks up the IAM
instance profile or ECS task role automatically. The only prod config addition is the region.
