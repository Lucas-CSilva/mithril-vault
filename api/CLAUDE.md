# Mithril Vault — Backend (`api/`)

Reactive Spring Boot service. Java 21, WebFlux (Project Reactor), MongoDB reactive driver.
Architecture is governed by `docs/architecture-contract.md`; this file is the working summary.
**Multi-user app** — read P2 (tenancy) below before touching any user-owned data.

## Stack

- Java **21** (toolchain), Spring Boot **4.0.6** (BOM-managed; starters carry no explicit version).
- WebFlux + Project Reactor, MongoDB reactive, Spring Security (OAuth2 resource server / JWT),
  springdoc-openapi, MapStruct, Lombok.
- Build: Gradle with version catalog (`gradle/libs.versions.toml`), dependency locking, Spotless.

## Hexagonal architecture (MANDATORY)

The domain is **technology-agnostic, not import-free** (see dependency table below — Reactor and
annotations are allowed; Spring/web/Mongo are not).

```
com.mithrilvault.api/
├── domain/              # Business core — no Spring, no web, no Mongo
│   ├── model/           # Aggregates / value objects (Records / @Value) — carry ownerId
│   ├── ports/           # Write & read interfaces (*Repository, *ReadRepository, *Gateway)
│   ├── command/         # Immutable Command Records
│   ├── commandhandler/  # *CommandHandler — validates, applies rules, persists via write ports
│   ├── query/           # *Query objects
│   ├── queryhandler/    # *QueryHandler / read services (derived values live here)
│   ├── exception/       # Domain exceptions
│   └── config/          # Domain-local config
├── application/         # Input adapters
│   ├── controllers/     # @RestController — bind body→command/query, dispatch, return result
│   ├── response/        # *Response Records — only when domain isn't a safe response shape
│   └── mapper/          # domain → response (only where a *Response exists)
└── infrastructure/      # Output adapters
    ├── persistence/     # Mongo documents, repositories, aggregation pipelines
    ├── config/          # Spring / WebFlux / security / Mongo / transactions
    └── mapper/          # Persistence entity ↔ Domain (MapStruct)
```

**Domain dependency policy (enforce via ArchUnit):**

| Domain MAY use | Domain MUST NOT use |
|---|---|
| JDK, Project Reactor (`Mono`/`Flux`) | Spring Framework/Boot (`@Component`/`@Service`/`@Autowired`, context) |
| Jakarta Validation, Lombok & MapStruct annotations | Spring Data, Mongo driver, `@Document`/Spring `@Id` |
| small pure utils (justified) | web/HTTP types; application DTOs |

- `application → domain` (+ Spring Web, validation); `infrastructure → domain` (+ Spring Data, Mongo).
- **Forbidden:** `domain → application|infrastructure`; `application → infrastructure`.

## P2 — Tenancy (MANDATORY, multi-user)

- Every user-owned aggregate carries an immutable `ownerId`.
- The caller's id comes from the verified JWT via the Reactor `Context` / reactive
  `SecurityContext` — **never** from request body/path/query.
- Every read **and** write is scoped to `ownerId`; read ports take `ownerId` and filter on it.
  An unscoped query of a user-owned collection is a defect.
- Not-owned resource → **404** (not 403). System data (`ownerId == null`, e.g. system categories)
  is read-only to all.
- Each user-owned aggregate needs a cross-tenant isolation test (A cannot see/modify B's data).

## Command / Query separation

```
WRITE: HTTP → Controller → Command → CommandHandler → write Port → repo impl → MongoDB
READ : HTTP → Controller → Query   → QueryHandler   → read  Port → aggregation → projection
```

Controllers dispatch commands/queries only — no business logic, no repository calls. Reads have
an explicit home (`query`/`queryhandler`); they must **not** be ad-hoc controller→repository
calls. **Derived values (`currentBalance`, invoice `totalAmount`, budget `spentAmount`, "Saldo
Líquido") are computed by the read side via Mongo aggregation and never persisted.**

## Data integrity & atomicity

- Mongo runs as a replica set (single-node locally) so reactive multi-document transactions work.
- Multi-document operations **MUST** be transactional: transfers (two legs), invoice payment
  (status → PAID + DEBIT txn), recurring regeneration, category-delete + reassign to "Outros".
- Money-moving / import commands **MUST** be idempotent (natural key: `transferPairId`,
  `importHash`/`FITID`, or an idempotency key). Aggregates with concurrent edits use `@Version`.

## Reactive rules (strict)

- **Never** `.block()` / `.blockFirst()` / `.blockLast()` in production code — stay in the chain.
- Return `Mono<T>` / `Flux<T>`; controllers return `Mono<ResponseEntity<T>>`.
- Prefer `Mono.empty()` over null; `Optional<T>` only for synchronous helpers.
- Error handling via operators (`onErrorResume`, `onErrorMap`), not `try/catch`.
- **Correlation IDs / tracing are handled by Micrometer Observation + Micrometer Tracing, not by
  hand-written `Mono.deferContextual` plumbing** (contract P13, ADR-001). Enable
  `Hooks.enableAutomaticContextPropagation()` once at startup so the trace/observation context (and
  thus the `correlationId` MDC key) follows the chain across thread hops automatically. Do not read
  or write the correlation id from the Reactor `Context` by hand. `deferContextual` remains fine for
  genuinely app-specific context that is not the trace/correlation id.
- Test reactive streams with `StepVerifier`; assertions via AssertJ.

## Immutability & type safety

- Domain models, commands, queries, and `*Response` DTOs are **Records** (or Lombok `@Value`).
  Input validation (Jakarta annotations) lives on the command/query Record.
- Constructor injection only with `final` fields (`@RequiredArgsConstructor` or explicit ctor).
  **Forbidden:** field injection (`@Autowired` on fields).
- Lombok allowed: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`.
  **Forbidden:** `@Data` on `@Document` classes; `@Component` in the domain layer.

## Money

`Long` centavos everywhere. **Never** `BigDecimal`, `double`, or `float`. Multiply before
dividing for percentages (see root `CLAUDE.md`). MongoDB stores `Int64`, never `Double`.

## Inbound & responses (no request DTOs)

- **Inbound:** bind the HTTP body/params straight to the `*Command` / `*Query` Record and
  dispatch it. **No `*Request` DTO** that just mirrors the command — it adds a layer for its own
  sake. Validation annotations go on the command/query.
- **Responses — least disclosure:** return the **domain model directly** when it has no sensitive
  fields and no fields the client doesn't need. Otherwise return a `*Response` Record exposing
  only what's needed. Responses **MUST NOT** leak sensitive data (password hashes, other users'
  data) or plumbing (`ownerId`, audit, `@Version`).
- **Mapping:** the **domain owns no mappers**. domain → `*Response` lives in `application.mapper`;
  persistence entity ↔ domain in `infrastructure.mapper` (MapStruct). A domain↔DTO mapper in
  `domain` would force `domain → application` — forbidden.

## Naming conventions

| Kind | Pattern |
|---|---|
| Commands | `Create*Command`, `Update*Command`, `Delete*Command` |
| Queries / handlers | `*Query` / `*QueryHandler` |
| Handlers | `*CommandHandler` |
| Write ports | `*Repository`, `*Gateway` (interfaces) |
| Read ports | `*ReadRepository`, `*Projection` |
| Controllers | `*Controller` (singular resource, e.g. `AccountController`) |
| Inbound | bind directly to `*Command` / `*Query` (no `*Request` DTO) |
| Response DTOs | `*Response` (only when projecting away sensitive/unneeded fields) |
| Mappers | `*Mapper` |
| Exceptions | `*NotFoundException`, `*ValidationException`, `*DomainException` |

## Testing

- **Unit** (`*Test`): pure domain tests, no Spring context, mock ports; `StepVerifier` for
  reactive flows, AssertJ for assertions.
- **Integration** (`*IT`): Testcontainers MongoDB (**as a replica set**) for persistence +
  aggregations; `@WebFluxTest` / `@SpringBootTest` for HTTP contracts. See `AbstractIntegrationTest`.
- **Tenancy:** each user-owned aggregate needs a cross-tenant isolation test (P2).
- Coverage % is advisory — gate on the presence of these test types, not a number.

## Schema-first

Define the OpenAPI contract (`specs/[###-feature]/contracts/[resource].openapi.yaml`) and the
MongoDB document schema (`specs/[###-feature]/data-model.md`, incl. `ownerId` indexes)
**before** implementing. Endpoints live under the base path `/api/mithril-vault/...` with **no
version in the URI** — API versioning, if ever needed, is negotiated via request header (contract §5).

## Commands

```bash
./gradlew test             # unit tests (*Test); excludes *IT
./gradlew integrationTest  # integration tests (*IT) with Testcontainers
./gradlew spotlessApply    # format (Google Java Format) — also runs on turn end via hook
./gradlew build            # full build
./gradlew bootRun          # run the API
```
