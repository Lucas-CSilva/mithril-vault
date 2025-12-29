<!--
Sync Impact Report:
Version: 1.0.0 → 1.1.0
Modified Principles:
  - Added Principle VIII: Schema-First Design
Added Sections:
  - API Contract Definition Requirements (Backend Architecture Rules)
Templates Requiring Updates:
  ✅ plan-template.md - Added schema-first gates
  ✅ tasks-template.md - Added schema definition phase
  ✅ spec-template.md - No changes needed
Follow-up TODOs: None
-->

# Mithril Vault Constitution

## Core Principles

### I. Hexagonal Architecture (Backend)
**MANDATORY** for all backend (`api/`) code. The domain layer MUST remain pure and independent:
- **Domain Layer** (`com.mithrilvault.api.domain`): Contains business logic, models, ports (interfaces), commands, and command handlers. MUST NOT depend on `application` or `infrastructure` layers.
- **Application Layer** (`com.mithrilvault.api.application`): Input adapters (REST controllers). Controllers receive HTTP requests, delegate to command handlers via ports, and return DTOs. MUST NOT contain business logic. MUST NOT directly access repositories or databases.
- **Infrastructure Layer** (`com.mithrilvault.api.infrastructure`): Output adapters (database repositories, external APIs). Implements domain ports. Contains all framework-specific configuration (Spring, MongoDB).

**Rationale**: Business rules are testable without spinning up servers or databases. Changes to persistence technology do not cascade into domain code. Clear separation enables domain model evolution independent of delivery mechanisms.

### II. CQRS Pattern (Backend)
**MANDATORY** for all state-changing operations:
- All write operations MUST be modeled as explicit `Command` objects in `domain.command`.
- Each command MUST have a dedicated `CommandHandler` in `domain.commandhandler`.
- Controllers MUST NOT directly invoke services; they send commands to handlers.
- Command handlers orchestrate domain logic and delegate persistence via ports.
- Queries (reads) MAY bypass CQRS for simplicity unless explicitly modeled otherwise.

**Rationale**: Commands provide clear audit trails and intent. Handlers centralize validation and business rule enforcement. Pattern supports future event sourcing or distributed transactions.

### III. Immutability & Type Safety (Backend)
**MANDATORY** for domain models and DTOs:
- Domain models MUST use Java Records or Lombok `@Value` for immutability.
- DTOs MUST use Records for all API contracts.
- Command objects MUST be immutable Records.
- Avoid setters in domain layer; use builder patterns or copy constructors only where Records are insufficient.
- Prefer `Optional<T>` over null returns.

**Rationale**: Immutable objects are thread-safe, easier to reason about, and prevent accidental state mutations. Type safety catches errors at compile time.

### IV. Mapping Isolation (Backend)
**MANDATORY**: Use MapStruct for all DTO ↔ Entity conversions:
- Place mappers in `domain.mapper` (domain-to-DTO) or `infrastructure.mapper` (entity-to-domain).
- MapStruct interfaces MUST be in appropriate layers respecting dependency rules.
- NEVER expose domain models directly in REST responses.
- NEVER pass DTOs into domain handlers.

**Rationale**: Explicit mapping prevents accidental coupling between API contracts and internal models. Changes to database schema do not leak into API. MapStruct generates efficient compile-time code.

### V. Feature-Sliced Design (Frontend)
**MANDATORY** for all frontend (`web/`) code. Use hybrid FSD + Hexagonal:
- **`src/app/`**: Next.js routing ONLY. Pages delegate immediately to feature components. No business logic.
- **`src/features/`**: Business verticals (accounts, cards, dashboard, investments, planning, subscriptions). Each feature is self-contained with its own components, hooks, and view models.
- **`src/core/`**: Hexagonal infrastructure. Contains `contexts` (dependency injection), `ports` (interfaces for external dependencies like API clients), and `services` (port implementations).
- **`src/shared/`**: Pure UI components and utilities with no domain knowledge (buttons, modals, formatters).

**Rationale**: Features can be developed independently. Switching backends (REST → GraphQL) requires only updating `core/services` implementations. Shared components remain reusable across features.

### VI. Styling & UI Consistency (Frontend)
**MANDATORY**: Use Tailwind CSS with strict Nord Theme adherence:
- **Colors**: Backgrounds use Snow Storm palette (light/white). Primary actions use Frost (blues/cyans). Semantic indicators use Aurora (red=error, green=success, orange=warning).
- All styling MUST use Tailwind utility classes. No inline styles except for dynamic calculations (charts).
- Custom components MUST be built with shadcn/ui or similar Tailwind-compatible libraries.
- Font stack: Geist Sans (primary), Geist Mono (code/numeric).

**Rationale**: Consistent visual language reduces cognitive load. Utility-first approach prevents CSS bloat. Nord Theme provides accessibility with high contrast.

### VII. Testing Discipline
**MANDATORY** testing strategy per layer:
- **Domain (Backend)**: Pure unit tests. Test domain logic without Spring context. Mock ports. Target 90%+ coverage for domain layer.
- **Infrastructure (Backend)**: Integration tests with testcontainers (MongoDB). Verify repository implementations against real database.
- **Application (Backend)**: Integration tests with `@WebFluxTest`. Test HTTP contracts and error handling.
- **Frontend**: Component tests with React Testing Library. Test user interactions, not implementation details. Visual regression tests for design system components.

**Rationale**: Fast domain tests provide immediate feedback. Infrastructure tests catch database contract issues. Component tests ensure UI behavior correctness.

### VIII. Schema-First Design
**MANDATORY** for all API and data modeling:
- **API Contracts**: OpenAPI 3.0+ specifications MUST be defined before implementation. Store in `/specs/[###-feature]/contracts/` directory.
- **Database Schemas**: MongoDB document schemas MUST be documented before coding repositories. Include field types, constraints, indexes, and relationships.
- **Type Definitions**: Frontend TypeScript interfaces MUST be generated from or aligned with backend API contracts.
- **Contract Testing**: API contract tests MUST verify implementation matches specification.
- **Workflow**: Schema Definition → Review → Test Generation → Implementation.

**Rationale**: Contracts serve as executable documentation. Frontend and backend teams can work in parallel once contracts are agreed upon. Breaking changes are caught early through contract testing. Schema evolution is tracked and intentional.

## Backend Architecture Rules

### Package Structure Enforcement
```
com.mithrilvault.api/
├── domain/
│   ├── model/          # Domain entities (Records or @Value)
│   ├── ports/          # Interfaces for external dependencies
│   ├── command/        # Command objects (immutable Records)
│   ├── commandhandler/ # Command handlers (orchestrate domain logic)
│   ├── exception/      # Domain-specific exceptions
│   ├── mapper/         # Domain ↔ DTO mappers (MapStruct)
│   └── config/         # Domain-specific configs (e.g., MapperConfig)
├── application/
│   ├── controllers/    # REST endpoints (@RestController)
│   ├── dto/            # Request/Response DTOs (Records)
│   └── mapper/         # DTO ↔ Command mappers (if needed)
└── infrastructure/
    ├── persistence/    # MongoDB repositories, entities
    ├── config/         # Spring, WebFlux, database configs
    └── mapper/         # Database entity ↔ Domain mappers
```

### Dependency Flow Rules
**MUST ENFORCE** via ArchUnit tests (T005 in Phase 1):
- `domain` imports: ONLY JDK, domain subpackages, and library annotations (MapStruct, Lombok).
- `application` imports: `domain`, Spring Web, validation libraries.
- `infrastructure` imports: `domain`, Spring Data, MongoDB, external SDKs.
- **FORBIDDEN**: `domain` importing `application` or `infrastructure`.
- **FORBIDDEN**: `application` importing `infrastructure`.

### Naming Conventions (Backend)
- Commands: `Create*Command`, `Update*Command`, `Delete*Command`
- Command Handlers: `*CommandHandler` (implements handling logic)
- Domain Ports: `*Repository`, `*Service`, `*Gateway` (interfaces)
- Controllers: `*Controller` (singular resource name, e.g., `AccountController`)
- DTOs: `*Request`, `*Response`, `*DTO`
- Mappers: `*Mapper` (MapStruct interfaces)
- Exceptions: `*NotFoundException`, `*ValidationException`, `*DomainException`

### Data Flow (Backend)
```
HTTP Request
  ↓
Controller (application.controllers)
  ↓ (directly receives the Command)
CommandHandler (domain.commandhandler)
  ↓ (validates, applies business rules)
Port Interface (domain.ports)
  ↓ (abstracts persistence)
Repository Impl (infrastructure.persistence)
  ↓
MongoDB
```

### API Contract Definition Requirements
**OpenAPI Specification** (MANDATORY before implementation):
- Store contracts in `/specs/[###-feature]/contracts/[resource].openapi.yaml`
- Include: paths, request/response schemas, status codes, error responses, security requirements
- Use `$ref` for shared schemas to avoid duplication
- Version APIs in path: `/api/v1/...`
- Document validation rules (min/max, patterns, required fields)

**MongoDB Schema Documentation** (MANDATORY before repository implementation):
- Document in `/specs/[###-feature]/data-model.md`
- Include: collection name, document structure, field types, indexes, unique constraints
- Define relationships (references, embedded documents)
- Specify TTL indexes for temporal data
- Document migration strategy for schema changes

**Contract Validation Tools**:
- Backend: Spring RestDocs or Springdoc OpenAPI for contract verification
- Frontend: TypeScript types generated from OpenAPI using openapi-typescript or similar
- CI/CD: Contract tests run before merge to verify specification compliance

## Frontend Architecture Rules

### Directory Structure Enforcement
```
src/
├── app/                    # Next.js App Router (routing only)
│   ├── layout.tsx          # Root layout (metadata, fonts)
│   ├── page.tsx            # Dashboard entry
│   └── [feature]/          # Feature routes (delegate to features/)
├── features/               # Business verticals
│   ├── accounts/           # Account & transaction management
│   ├── cards/              # Card management
│   ├── dashboard/          # Dashboard & overview
│   ├── investments/        # Investment hub
│   ├── planning/           # Budget & goals
│   └── subscriptions/      # Subscription management
├── core/                   # Hexagonal infrastructure
│   ├── contexts/           # React contexts for DI
│   ├── ports/              # Interfaces (e.g., IApiClient)
│   └── services/           # Port implementations
└── shared/                 # Pure UI components
    ├── components/         # Reusable UI (Button, Modal, Card)
    └── utils/              # Formatters, validators, helpers
```

### Feature Module Structure
Each feature in `src/features/[name]/` MUST follow:
```
[feature]/
├── components/         # Feature-specific components
├── hooks/              # Feature-specific React hooks
├── types.ts            # Feature TypeScript types/interfaces
├── api.ts              # API client calls (uses core/ports)
└── index.tsx           # Feature entry point/barrel export
```

### Dependency Flow Rules (Frontend)
- `app/` imports: `features/`, `shared/`, Next.js APIs.
- `features/` imports: `core/` (via ports), `shared/`, React.
- `core/` imports: ONLY external libraries (e.g., axios), React.
- `shared/` imports: ONLY React, Tailwind utilities. NO feature or core imports.
- **FORBIDDEN**: `core/` or `shared/` importing `features/`.
- **FORBIDDEN**: `features/` directly importing other `features/` (use shared if needed).

### Naming Conventions (Frontend)
- Components: PascalCase (`AccountCard`, `TransactionList`)
- Hooks: `use*` prefix (`useAccounts`, `useTransactions`)
- Contexts: `*Context` (`AuthContext`, `ThemeContext`)
- API functions: camelCase (`fetchAccounts`, `createTransaction`)
- Types/Interfaces: PascalCase with `I` prefix for interfaces (`IAccount`, `ITransaction`)

### State Management (Frontend)
- **React Context** for global state (auth, theme, feature flags).
- **React Query / SWR** for server state (API data fetching, caching).
- **Local state** (`useState`) for UI-only state (modals, forms).
- AVOID Redux unless explicitly justified for complex workflows.

## Dependency Management

### Backend (Java 21)
**Locked Versions** (update only via explicit constitution amendment):
- Spring Boot: `4.0.1`
- Java: `21` (LTS)
- MapStruct: `1.6.3`
- Lombok: (use Spring-managed version)
- MongoDB Reactive Driver: (use Spring-managed version)
- Testing: JUnit 5, Spring Boot Test, Testcontainers

**Approved Libraries**:
- Validation: Jakarta Validation API
- Reactive: Project Reactor (via Spring WebFlux)
- JSON: Jackson (via Spring Boot)
- Logging: SLF4J + Logback (via Spring Boot)

**Prohibited**:
- `@Autowired` field injection (use constructor injection)
- `@Component` in domain layer (domain must be POJO)
- Blocking JDBC drivers (reactive stack only)

### Frontend (TypeScript 5, Next.js 16, React 19)
**Locked Versions**:
- Next.js: `16.1.1`
- React: `19.2.3`
- TypeScript: `5.x`
- Tailwind CSS: `4.x`
- Node: `20.x` (LTS)

**Approved Libraries**:
- Styling: Tailwind CSS, @tailwindcss/postcss
- HTTP: Fetch API (built-in), SWR or React Query
- Forms: React Hook Form + Zod validation
- Charts: Recharts, Chart.js
- UI Primitives: shadcn/ui (Radix UI + Tailwind)

**Prohibited**:
- CSS-in-JS libraries (styled-components, emotion)
- Global CSS files outside `globals.css`
- jQuery or legacy DOM manipulation libraries

## Testing & Quality Standards

### Code Coverage Targets
- Backend domain layer: ≥90%
- Backend application layer: ≥75%
- Backend infrastructure layer: ≥60%
- Frontend features: ≥70%
- Frontend shared components: ≥80%

### Code Quality Tools
**Backend**:
- Formatter: Spotless (Google Java Format)
- Linter: Checkstyle (future)
- Architecture Tests: ArchUnit (enforce layer boundaries)
- Mutation Testing: PITest (future enhancement)

**Frontend**:
- Formatter: Prettier (via Next.js)
- Linter: ESLint (via Next.js eslint-config)
- Type Checking: TypeScript strict mode (`"strict": true`)

### Performance Benchmarks
- Backend API response time: p95 < 200ms (local dev), p95 < 500ms (prod)
- Frontend Time to Interactive (TTI): < 2s on 4G
- Frontend Lighthouse Score: Performance ≥90, Accessibility ≥95

## Security & Compliance

### Data Privacy
- **NO** storage of card numbers (PAN).
- **NO** storage of CVV codes.
- Passwords MUST be hashed (BCrypt or Argon2).
- API keys stored in environment variables, NEVER committed.

### API Security
- All endpoints MUST require authentication (except `/health`, `/login` and ``/register`).
- Use JWT tokens with expiration.
- Rate limiting on authentication endpoints (5 attempts per minute).
- CORS restricted to known frontend origins.

### Observability
- Structured JSON logging (use Logback with JSON encoder).
- Log correlation IDs for request tracing.
- Health check endpoint: `/actuator/health` (Spring Boot Actuator).
- Metrics: Micrometer with Prometheus exporter (future).

## Versioning & Breaking Changes

### Semantic Versioning (SemVer)
**Backend**: API uses versioning in URL (`/api/v1/accounts`).
- MAJOR: Endpoint removed or request/response schema incompatibly changed.
- MINOR: New endpoint added or backward-compatible field added.
- PATCH: Bug fix with no API contract change.

**Frontend**: Internal versioning (not exposed to users).
- MAJOR: Rewrite of feature architecture.
- MINOR: New feature module added.
- PATCH: Bug fix or UI refinement.

**Constitution**: (this document)
- MAJOR: Principle removed or redefined in incompatible way.
- MINOR: New principle added or materially expanded.
- PATCH: Clarifications, typos, wording refinements.

### Deprecation Policy
- Backend endpoints: Minimum 2 minor versions before removal (announce in changelog).
- Frontend components: Minimum 1 minor version deprecation period (console warnings).

## Governance

### Constitution Authority
This constitution supersedes all other documentation, coding standards, and verbal agreements. In conflicts between constitution and implementation, constitution prevails.

### Amendment Process
1. Propose change in issue with `constitution-amendment` label.
2. Requires approval from Principal Software Architect (or 2/3 majority if team voting).
3. Update version in this file following SemVer rules.
4. Update `LAST_AMENDED_DATE`.
5. Propagate changes to templates (`plan-template.md`, `spec-template.md`, `tasks-template.md`).
6. Document in Sync Impact Report (HTML comment at top of file).

### Compliance Verification
- All PRs MUST include constitution compliance checklist.
- Automated checks: ArchUnit tests, ESLint rules, TypeScript compiler.
- Manual review: Architect verifies principles I-VII adherence.
- Violations MUST be justified in `Complexity Tracking` section of implementation plan.

### Guidance Files
For runtime development guidance, refer to:
- Feature specifications: `/specs/[###-feature]/spec.md`
- Implementation plans: `/specs/[###-feature]/plan.md`
- Task breakdowns: `/specs/[###-feature]/tasks.md`
- Command workflows: `.github/prompts/speckit.*.prompt.md`

**Version**: 1.1.0 | **Ratified**: 2025-12-28 | **Last Amended**: 2025-12-28
