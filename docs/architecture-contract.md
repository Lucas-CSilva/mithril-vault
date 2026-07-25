# Mithril Vault — Architecture Contract

**Version:** 1.4.0
**Status:** Authoritative engineering reference (the single source of truth for architecture).
**Relationship to product:** `docs/product-definition.md` defines *what* we build.
This contract defines *how*. One intentional divergence: the product was scoped single-user;
this project is built **multi-user** (a deliberate learning goal — see §1 P2 and §8).

---

## How to read this

Rules use RFC-2119 force. **MUST** = enforced (CI/review blocks merge). **SHOULD** = strong
default; deviations are justified in the PR. **MAY** = allowed.

The guiding bias of this contract: **specify the hard parts precisely** (money, multi-user
isolation, derived reads, atomicity, enforcement) and **stay pragmatic on the rest** (domain
purity, versions, coverage). Rules you cannot enforce or realistically follow are not rules.

---

## 1. Core Principles

### P1 — Money is integer minor units
All monetary values are `Long` **centavos** (1 BRL = 100) at every layer: Java `Long`, MongoDB
`Int64` (never `Double`), JSON integer, frontend a branded `Centavos` (a `number` newtype, see
§3) formatted only at render. **MUST NOT** use `BigDecimal`, `double`, or `float` for money
anywhere. Percentage/proportion math **MUST** multiply before dividing.

```java
long ir = (grossYield * 225) / 1000;   // ✓ 22.5%
long ir = grossYield / 1000 * 225;     // ✗ truncates
```

*Rationale:* the product's #1 data-integrity invariant. It belongs in the contract, not in a
tool config.

### P2 — Multi-user by default; tenancy is enforced at the data edge
Every user-owned aggregate **MUST** carry an immutable `ownerId` (the User id). The
authenticated user id **MUST** be derived server-side from the verified JWT (via the Reactor
`Context` / reactive `SecurityContext`) and **MUST NEVER** be read from the request body, path,
or query. Every read and write **MUST** be scoped to the caller's `ownerId`.

- Read ports **MUST** take `ownerId` and filter on it; an unscoped query of a user-owned
  collection is a defect.
- Accessing a resource the caller does not own returns **404**, not 403 (do not leak existence).
- Global/system data (e.g. system `Category` with `ownerId == null`) is readable by all,
  writable by none.
- Every aggregate's integration tests **MUST** include a cross-tenant isolation case
  ("user A cannot see/modify user B's data").

*Rationale:* in a multi-user finance app, authorization *is* data scoping. Bolting it on per
endpoint guarantees a leak; pushing `ownerId` into the port contract makes the safe path the
default path.

### P3 — Hexagonal architecture with a *pragmatic* domain
Three layers: `domain` (business core), `application` (input adapters / HTTP), `infrastructure`
(output adapters / persistence, external). Dependencies point inward.

The domain is **technology-agnostic, not import-free.** It encodes business rules independent of
Spring, the web, and the database — but it may use the async primitives and annotations that are
part of our core programming model. The followable line:

| Domain MAY depend on | Domain MUST NOT depend on |
|---|---|
| JDK | Spring Framework / Boot (`@Component`, `@Service`, `@Repository`, `@Autowired`, context) |
| Project Reactor (`Mono`/`Flux`) | Spring Data, MongoDB driver, persistence annotations (`@Document`, Spring `@Id`) |
| Jakarta Validation annotations | Web/HTTP types (servlet, `ServerRequest`, `ResponseEntity`) |
| Lombok & MapStruct annotations | Application response DTOs (`*Response`) |
| Small, pure utility libs (justified in PR) | Any concrete framework or I/O |

*Rationale:* a literally zero-import domain in a reactive Spring app is theatre — you either
ban `Mono` from ports (and pay everywhere) or quietly cheat. We allow Reactor and annotations,
forbid the things that actually couple business rules to a framework or to delivery.

### P4 — Command / Query separation (the realistic version)
We separate **write** and **read** code paths. We do **not** mandate separate datastores or
event sourcing — and we do **not** add a parallel inbound DTO layer just to mirror the structure.

- **The inbound type *is* the `*Command` / `*Query`.** Controllers bind the HTTP body/params
  **directly** to the immutable `*Command` (writes) or `*Query` (reads) Record and dispatch it.
  There is **no separate `*Request` DTO** — a request DTO that merely duplicates a command's
  fields and is mapped 1:1 adds a layer for its own sake. Input validation (Jakarta annotations)
  lives on the command/query Record itself.
- **Writes:** a `*Command` is handled by a `*CommandHandler` that validates, applies rules, and
  persists via write ports. Controllers contain no business logic and never touch repositories.
- **Reads:** a `*Query` is handled by a `*QueryHandler` (or read service) behind dedicated **read
  ports** that return projections/read models. Reads **MUST NOT** be ad-hoc
  controller→repository calls.
- **Derived values are computed, never stored, and have one owner.** `currentBalance`, invoice
  `totalAmount`, budget `spentAmount`, goal projections, and dashboard "Saldo Líquido" are
  produced by read-side aggregation (MongoDB aggregation pipelines in `infrastructure`, exposed
  through read ports). No code path may persist a derived value.

*Rationale:* this domain is read-dominated; its hard problems are aggregations. We give reads a
defined, testable home without the cost of full CQRS, and we avoid the boilerplate of a
request-DTO tier that is structurally identical to the command/query it feeds.

### P5 — Immutability & type safety
Domain models, commands, queries, and response DTOs **MUST** be Records (or Lombok `@Value`).
Constructor injection only, `final` fields; field injection (`@Autowired` on fields) is
forbidden. `@Data` on persistence documents is forbidden. "Immutable" means values are not
mutated in place — state transitions produce new instances / new persisted versions
(e.g. recurring-series edits regenerate future instances; past instances are immutable).
Use `Mono.empty()`/`Flux` to model absence in reactive code; `Optional<T>` is for **synchronous
helpers only** (never inside a reactive chain).

### P6 — Mapping & response shaping
The **domain owns no mappers.** Mapping only exists where there is a real shape difference —
not to satisfy symmetry.

- **Inbound:** none. The HTTP body binds straight to the `*Command`/`*Query` Record (P4).
- **Outbound:** a domain model **MAY** be returned directly as the response **iff** it contains
  no sensitive fields **and** no fields the client does not need. Otherwise return a `*Response`
  Record that exposes only what's needed; that domain → response mapper lives in
  `application.mapper` (MapStruct). `infrastructure.mapper` holds persistence entity ↔ domain.
- **Enforced invariant — least disclosure:** responses **MUST NOT** contain sensitive fields
  (e.g. password hashes, another user's data, internal `ownerId`/audit/`@Version` plumbing) and
  **MUST NOT** contain fields irrelevant to the consumer. When in doubt, project.

*Rationale:* returning the domain model directly is fine when it's already the right, safe shape
— a 1:1 "response DTO" then adds nothing. The rule we actually care about is what leaves the
system, so we enforce the field-exposure invariant rather than a blanket "never expose the
domain" ceremony. (A domain↔DTO mapper placed in `domain` would force `domain → application`,
breaking P3 — hence mappers stay in `application`/`infrastructure`.)

### P7 — Data integrity & atomicity
Money is only correct if multi-document operations are atomic.

- MongoDB **MUST** run as a replica set (single-node is fine for local dev) so reactive
  multi-document transactions are available. Standalone Mongo is not a supported configuration.
- Operations that touch more than one document **MUST** be transactional: account transfers
  (two linked transactions), invoice payment (invoice → PAID **and** the DEBIT transaction),
  recurring-series regeneration, category deletion + reassignment to "Outros".
- Money-moving and import commands **MUST** be idempotent, keyed on a natural id
  (`transferPairId`, transaction `importHash` / OFX `FITID`) or a client-supplied idempotency
  key. Re-submitting the same operation **MUST NOT** double-apply.
- Aggregates exposed to concurrent edits **MUST** use optimistic locking (`@Version`).

### P8 — Schema-first
API contracts (OpenAPI 3.0+) and MongoDB document schemas **MUST** be defined and reviewed
before implementation: `specs/[###-feature]/contracts/[resource].openapi.yaml` and
`specs/[###-feature]/data-model.md` (collections, fields, types, **indexes** incl. the
`ownerId` indexes required by P2, unique constraints, relationships). Frontend TS types
**MUST** align with these contracts — hand-written to mirror the backend Records (§3.2), with the
review confirming agreement. Workflow: schema → review → tests → implementation.

### P9 — Feature-Sliced + hexagonal frontend
`app/` (routing only) → `features/<vertical>/` (self-contained: `components/`, `hooks/`,
`types.ts`, `api.ts`, `keys.ts`, `schema.ts`) → `core/` (`ports/`, `services/`) →
`shared/` (`components/ui`, `hooks`, `utils`). Boundaries: `shared`/`core` **MUST NOT** import
`features`; a feature **MUST NOT** import another feature (extract to `shared`). Features depend
on `core` **ports** (interfaces) and import `core/services` singletons directly — no context-based
DI container. Encapsulation is enforced by `eslint-plugin-boundaries`, not by barrel `index.ts`
files. Full structure, ports, data-layer and auth conventions in §3; day-to-day guide in `web/CLAUDE.md`.

### P10 — Styling: Nord theme, light mode
Tailwind utilities only; built on shadcn/ui. No CSS-in-JS, no global CSS outside `globals.css`.
Snow Storm backgrounds, Frost for primary/positive, Aurora for semantics. Palette in
`web/CLAUDE.md`.

### P11 — Testing discipline (types mandated, percentages advisory)
What **MUST** exist, per layer:
- **Domain:** pure unit tests, no Spring context, mock ports; reactive flows via `StepVerifier`,
  assertions via AssertJ.
- **Infrastructure:** Testcontainers (MongoDB, **as a replica set**) integration tests for
  repositories and aggregations.
- **Application:** `@WebFluxTest` / slice tests for HTTP contracts, validation, and auth.
- **Tenancy:** every user-owned aggregate has a cross-tenant isolation test (P2).
- **Frontend:** React Testing Library — test behavior, not implementation.

Coverage percentages are **advisory signals, not merge gates.** We gate on the presence of the
mandated test types and on meaningful assertions, not on a number. (No mutation-testing mandate;
PITest interacts poorly with reactive chains.)

### P12 — Security
- Email + password; passwords hashed with BCrypt or Argon2. **Public registration is enabled**
  (multi-user). Auth: short-lived access JWT (~15 min) + rotating refresh token in an `httpOnly`,
  `Secure`, `SameSite` cookie (~30 day).
- All endpoints require a valid access token **except** the documented public set:
  `POST /register`, `POST /login`, `POST /refresh`, `/actuator/health`,
  `/actuator/info`, and the OpenAPI/Swagger paths. This set **MUST** match the application
  security config exactly.
- Rate-limiting on auth endpoints is **deferred** (out of scope for MVP). CORS restricted to known frontend origins.
- Never store card PAN or CVV. Never commit or log secrets/tokens. Treat all input as untrusted;
  validate at the edge (`application`), enforce invariants in the `domain`.

### P13 — Observability
- Structured JSON logging (Logback JSON encoder).
- Request correlation IDs **MUST** propagate via **Micrometer Observation / Micrometer Tracing**,
  **not** hand-rolled Reactor `Context` writes (`Mono.deferContextual`) and **not** raw
  thread-local MDC. Each request is wrapped in an `Observation` (the WebFlux server observation
  auto-configured by Boot Actuator); Micrometer Tracing's context-propagation bridge carries the
  trace/correlation id across reactor thread hops and exposes it to Logback via MDC keys
  (`traceId`/`spanId`, surfaced as `correlationId`). Hand-managing the id in the Reactor `Context`
  is forbidden — it duplicates machinery the observation stack already provides and silently drifts
  from the trace context. See `ADR-001`.
- Correlation IDs are **internal only** — they are not exposed in response headers or response bodies. The `correlationId` field is absent from `ErrorResponse` across all OpenAPI specs. Tracing remains fully functional server-side via Micrometer; the ID never leaves the system.
- Reactor context propagation **MUST** be enabled globally
  (`Hooks.enableAutomaticContextPropagation()`) so the observation/trace context is restored on
  every operator hop without per-handler `deferContextual` plumbing.
- Health via Spring Boot Actuator. Metrics via Micrometer (Prometheus) when introduced — the same
  Micrometer core the tracing bridge builds on.

---

## 2. Backend structure & conventions

```
com.mithrilvault.api/
├── domain/
│   ├── model/            # aggregates / value objects (Records, @Value) — carry ownerId
│   ├── ports/            # write & read interfaces (*Repository, *ReadRepository, *Gateway)
│   ├── command/          # *Command (immutable Records)
│   ├── commandhandler/   # *CommandHandler
│   ├── query/            # *Query objects
│   ├── queryhandler/     # *QueryHandler / read services
│   ├── exception/        # domain exceptions
│   └── config/           # domain-local config (e.g. MapperConfig)
├── application/
│   ├── controllers/      # @RestController — bind body→command/query, dispatch, return result
│   ├── response/         # *Response Records — only when the domain isn't a safe response shape
│   └── mapper/           # domain → response (only where a *Response exists)
└── infrastructure/
    ├── persistence/      # Mongo documents, repositories, aggregation pipelines
    ├── config/           # Spring, WebFlux, security, Mongo, transactions
    └── mapper/           # persistence entity ↔ domain
```

**Dependency flow** (enforced by ArchUnit): `domain` per the P3 table; `application → domain`
(+ Spring Web, validation); `infrastructure → domain` (+ Spring Data, Mongo). Forbidden:
`domain → application|infrastructure`; `application → infrastructure`.

**Naming**

| Kind | Pattern |
|---|---|
| Commands / handlers | `Create*Command` … / `*CommandHandler` |
| Queries / handlers | `*Query` / `*QueryHandler` |
| Write ports | `*Repository`, `*Gateway` |
| Read ports | `*ReadRepository`, `*Projection` |
| Inbound types | bind directly to `*Command` / `*Query` (no `*Request` DTO) |
| Controllers | `*Controller` (singular resource) |
| Response DTOs | `*Response` (only when projecting away sensitive/unneeded fields) |
| Mappers | `*Mapper` |
| Exceptions | `*NotFoundException`, `*ValidationException`, `*DomainException` |

---

## 3. Frontend structure & conventions
The frontend mirrors the backend's discipline: **Feature-Sliced Design over a hexagon.** Features
are vertical slices that depend *inward* on `core` ports; `core` holds the ports and their
service implementations; `shared` is leaf UI/utilities. Day-to-day guidance lives in
`web/CLAUDE.md`; the rules below are contract-level.

```
web/src/
├── app/                      # Next.js App Router — routing, layouts, RSC pages ONLY (no logic)
│   ├── (auth)/               # public route group (login, register)
│   ├── (app)/                # authenticated route group — gated by middleware
│   └── layout.tsx            # root providers (QueryClient, NotificationProvider)
├── features/<vertical>/      # accounts, transactions, budgets, invoices, dashboard, …
│   ├── components/           # feature UI (Server + Client components)
│   ├── hooks/                # React Query hooks (useAccounts, useCreateTransaction)
│   ├── api.ts                # calls core/services directly — NO raw fetch here
│   ├── keys.ts               # query-key factory for this feature
│   ├── types.ts              # hand-written types mirroring backend Records
│   └── schema.ts             # zod form schemas (when the feature has forms)
├── core/
│   ├── ports/                # interfaces: ApiClient, AuthGateway, … (typed contracts)
│   └── services/             # singleton implementations (HttpApiClient, …) — imported directly
└── shared/
    ├── components/ui/        # shadcn/ui primitives (@/shared/components/ui)
    ├── hooks/                # cross-feature hooks
    ├── types/                # Centavos brand and other cross-cutting types
    └── utils/                # formatBRL, reais↔centavos, date helpers
```

**Import boundaries** (MUST, enforced by `eslint-plugin-boundaries`): `shared` imports nothing
above it; `core` may import `shared` only; a feature may import `core` + `shared` but **never**
another feature (promote shared code to `shared`); `app` wires features together. Cross-feature
reuse goes through `shared` or a `core` port — never a deep import into another slice.

### 3.1 Core — ports and services
- **Ports** (`core/ports`) are TypeScript interfaces describing capabilities the app needs:
  `ApiClient` (typed HTTP), `AuthGateway` (login/refresh/logout), etc. These are the typed
  contracts features depend on — never on a concrete service or on `fetch` directly.
- **Services** (`core/services`) implement the ports and are exported as module singletons —
  e.g. `httpApiClient` (an `HttpApiClient` instance) wraps `fetch` with base URL, JSON,
  `credentials: 'include'`, and centralized error/401 handling (§3.4). Features import the
  singleton directly: `import { httpApiClient } from '@/core/services/httpApiClient'`.
- **No context-based DI container.** There is no `core/contexts` provider chain. The single
  `HttpApiClient` implementation is never swapped at runtime; React Query + MSW provides test
  isolation without provider ceremony. A feature **MUST NOT** call `fetch` directly — that is
  a lint error.

### 3.2 Types & money (hand-written, branded)
- Each feature **hand-writes** its `types.ts` to mirror the backend response Records exactly —
  same field names, same shape. We do not codegen; the manual mirror is the review checkpoint
  that FE and API agree. **No `any`**, **no `I` prefix** (`Account`, not `IAccount`).
- Money is a **branded `Centavos`** type, defined once in `shared/types`:

  ```typescript
  export type Centavos = number & { readonly __brand: 'Centavos' };
  export const centavos = (n: number): Centavos => n as Centavos;
  export const formatBRL = (value: Centavos): string =>
    (value / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  ```

  Every monetary field is typed `Centavos`, not `number`, so a raw `number` (e.g. a reais form
  value) cannot be passed where centavos are expected without an explicit conversion at the edge.
  `formatBRL` is the **only** place division by 100 happens; it runs at render only.

### 3.3 Data layer — React Query (the one client server-state lib)
- React Query is the single client-side server-state library (no SWR, no ad-hoc `useEffect`
  fetching). Server-state lives in the query cache; React state is for UI only.
- **Query keys** are produced by a per-feature factory in `keys.ts`, hierarchical and typed —
  e.g. `accountKeys.all = ['accounts']`, `accountKeys.detail(id) = ['accounts', id]`. Components
  **MUST NOT** hand-build key arrays inline.
- **Hook naming:** reads `use<Plural>()` / `use<Entity>(id)`; mutations `use<Verb><Entity>()`
  (`useCreateTransaction`). Feature hooks call `api.ts` (which calls the `core/services` singleton).
- **Invalidation discipline (MUST):** a mutation declares **every** query its write affects,
  including cross-feature ones. A transaction create/edit invalidates transactions **and**
  account balances, the relevant budget(s), and the dashboard. Derived reads are never patched
  by hand — they are refetched (consistent with P4: derived values have one owner, the read side).

### 3.4 Server/Client boundary & auth
- **Server Components by default.** `'use client'` only where hooks, event handlers, or browser
  APIs are required. Data for the initial render is fetched in RSC; interactive server-state uses
  React Query in client components.
- **Tokens live in `httpOnly`, `Secure`, `SameSite` cookies** (P12) — **never** in
  `localStorage`/JS-readable storage. The `ApiClient` sends `credentials: 'include'`; RSC fetches
  forward the incoming cookies.
- **401 handling is centralized in the `ApiClient`:** on a 401 it attempts a single
  `POST /refresh` and retries once; a failed refresh clears session and redirects to login.
  Features never implement refresh logic.
- **Route gating** is done in Next.js `middleware.ts` for the `(app)` group (redirect
  unauthenticated users to login) — defense in depth on top of the API's own authz (P2); the
  middleware is **not** the security boundary, the API is.

### 3.5 Forms, UI states & notifications
- **Forms:** react-hook-form + zod; the schema is co-located in the feature (`schema.ts`).
  Validation mirrors backend Jakarta constraints. Reais↔centavos conversion happens at the form
  edge (user types reais → convert to `Centavos` before sending; never round-trip through float
  beyond the single conversion).
- **Every async view models four states:** loading (Suspense + skeletons), error (error boundary
  with retry), empty, and loaded. Empty and error states are designed, not afterthoughts.
- **Notifications:** a single app-level notifications context drives toasts; budget threshold
  alerts (80% / 100%) and mutation success/failure surface through it, not via scattered local
  toast calls.

### 3.6 Enforcement (frontend)
- `eslint-plugin-boundaries` enforces the import rules above (including the no-cross-feature rule)
  and makes barrel `index.ts` files unnecessary — encapsulation is the lint rule's job.
  Raw `fetch` inside `features` is a lint error (must go through `api.ts` → `core/services`).
- TypeScript `strict`; `any` is disallowed. Monetary fields must be `Centavos`, not `number`.
- React Testing Library tests assert behavior (P11).

---

## 4. Dependency & version policy (policies, not pins)
Exact versions live in the tooling source of truth (`api/gradle/libs.versions.toml`,
`web/package.json`) — **not** in this contract, so routine bumps are not "amendments."

- **Backend:** Java = current LTS (21). Reactive-only — **no blocking JDBC / blocking drivers**.
  Spring versions BOM-managed. Approved: Jakarta Validation, Project Reactor, Jackson, SLF4J +
  Logback, MapStruct, Lombok (`@Getter/@Setter/@Builder/@RequiredArgsConstructor` only).
- **Frontend:** Node = current LTS. Tailwind utility-first. **No CSS-in-JS**, no jQuery. Forms
  via react-hook-form + zod; UI via shadcn/ui (Radix + Tailwind); charts via Recharts/Chart.js.
- **Package manager (web): pnpm.**

---

## 5. API & versioning
- **No version in the URI.** Paths stay clean under the base path (`/mithril-vault/...`); the
  URL identifies the resource, not its version. There is no `/v1/` path segment.
- **Versioning, when needed, is negotiated via request header** — a media-type/`Accept` or a
  dedicated version header (e.g. `Accept: application/vnd.mithrilvault.v2+json` or
  `X-API-Version: 2`). Unversioned requests resolve to the current default. Pin the header
  contract in the OpenAPI spec when a second version is actually introduced.
- SemVer for the contract surface: MAJOR = incompatible endpoint/schema change; MINOR =
  backward-compatible addition; PATCH = fix with no contract change. Deprecate ≥2 minor versions
  before removal.

---

## 6. Enforcement & quality gates
A rule is only real if a machine or a reviewer checks it.

- **Backend boundaries:** ArchUnit tests enforce §2 dependency flow and the P3 domain table.
- **Frontend boundaries:** ESLint `eslint-plugin-boundaries` (or `import/no-restricted-paths`)
  enforces §P9 import rules.
- **Format:** Spotless (Google Java Format) for `api/`; Prettier for `web/` (both wired as
  Claude format-on-edit hooks).
- **Types/lint:** TS strict mode; ESLint (Next config).
- **Tests:** the mandated test types in P11 must be present; tenancy isolation tests required
  for user-owned aggregates.
- **Response exposure (P6):** review that responses carry no sensitive fields and no fields the
  client doesn't need (least disclosure) — whether returning a domain model or a `*Response`.
- **PR checklist:** money rule (P1), tenancy scoping (P2), no derived value persisted (P4),
  response least-disclosure (P6), transactional + idempotent money ops (P7), public-path set
  matches config (P12).

---

## 7. Change process (lightweight)
Amend via PR editing this file: state the change, the rationale, and bump the version (SemVer:
MAJOR removes/redefines a principle; MINOR adds/expands; PATCH clarifies). No separate approval
ceremony beyond normal PR review. Version pins and task ids do **not** belong here, so they never
trigger an amendment.

---

## 8. Multi-user migration status
- ✅ `docs/product-definition.md` v1.2: multi-user + public registration, a `User` entity, an
  `ownerId` on every owned entity, and a *Multi-tenancy & Data Isolation* section.
- ✅ `docker-compose.yml`: Mongo runs as a single-node **replica set** (`rs0`) with keyfile auth +
  a `mongo-init` one-shot — local multi-document transactions now possible (P7).
- ✅ **App config:** `api/.../application.yaml` Mongo URI includes `replicaSet=rs0` so the driver
  uses replica-set topology (otherwise transactions error). Property path is **`spring.mongodb.uri`**
  — Spring Boot 4 dropped the `spring.data.` prefix; do not "fix" it back.
- ⬜ Define the `ownerId` indexes per collection in each feature's `data-model.md` (P8).
- ✅ Replaced `@AuthenticationPrincipal(expression = "subject")` (a SpEL string re-typed in every
  controller) with a custom `@CurrentOwnerId` argument annotation
  (`application/security/CurrentOwnerId.java`) backed by a reactive `HandlerMethodArgumentResolver`
  (`infrastructure/config/CurrentOwnerIdArgumentResolver.java`, registered in
  `infrastructure/config/WebFluxConfig.java`) that reads `ReactiveSecurityContextHolder` and calls
  `Jwt.getSubject()` directly. Removes the runtime-only failure mode of a typo'd SpEL expression
  and centralizes the P2 identity-extraction point. All controllers use `@CurrentOwnerId String
  ownerId` going forward.
