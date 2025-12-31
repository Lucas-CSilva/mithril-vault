# Implementation Plan: Development Environment Setup

**Branch**: `001-environment-setup` | **Date**: December 31, 2025 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-environment-setup/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Configure development environment for both frontend and backend to enable immediate productivity for new developers. Frontend setup includes Next.js App Router, Tailwind CSS with Nord theme palette, ESLint configuration, and Shadcn UI integration. Backend setup includes MongoDB connection with reactive drivers, Spring Boot profiles for different environments, CORS policies, health checks, and comprehensive logging. This is a foundational infrastructure feature that enables all future development work.

## Technical Context

**Backend**:
- **Language/Version**: Java 21 (LTS)
- **Framework**: Spring Boot 4.0.1 with WebFlux (reactive stack)
- **Primary Dependencies**: Spring Data MongoDB Reactive, MapStruct 1.6.3, Lombok, Spring Boot Actuator
- **Storage**: MongoDB (via docker-compose for local development)
- **Testing**: JUnit 5, Spring Boot Test, Testcontainers for MongoDB
- **Build Tool**: Gradle 8.x
- **Code Quality**: Spotless (Google Java Format)

**Frontend**:
- **Language/Version**: TypeScript 5.x
- **Framework**: Next.js 16.1.1 with App Router, React 19.2.3
- **Primary Dependencies**: Tailwind CSS 4.x, shadcn/ui (Radix UI primitives), PostCSS
- **UI Library**: shadcn/ui with Nord theme customization
- **Testing**: React Testing Library, Jest (via Next.js)
- **Package Manager**: pnpm
- **Node Version**: 20.x (LTS)

**Project Type**: Full-stack monorepo (web + api)

**Performance Goals**:
- Frontend dev server hot-reload: < 2 seconds
- Backend API startup: < 30 seconds with database connection
- New developer onboarding: < 15 minutes to running stack

**Constraints**:
- Must use reactive patterns (no blocking I/O in backend)
- Must adhere to Nord theme color palette
- Must follow hexagonal architecture (backend) and FSD (frontend)
- All configuration must be externalized (environment variables)

**Scale/Scope**:
- 2 projects (api/, web/)
- ~15-20 configuration files
- Docker-compose with 1 service (MongoDB)
- Environment variable documentation for 10-15 settings

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Backend Features** (`api/`):
- [x] **Principle I**: Domain layer has zero dependencies on Spring/MongoDB (N/A - configuration only, no domain logic)
- [x] **Principle II**: Write operations modeled as Commands with dedicated CommandHandlers (N/A - configuration only)
- [x] **Principle III**: Domain models use Records or Lombok @Value (immutable) (N/A - configuration only)
- [x] **Principle IV**: MapStruct mappers isolate DTO ↔ Entity conversions (N/A - configuration only)
- [x] **Principle VII**: Domain tests are pure unit tests (no Spring context) (N/A - configuration only)
- [x] **Principle VIII**: OpenAPI contract defined in `/specs/[###-feature]/contracts/` before implementation (N/A - infrastructure only)
- [x] **Principle VIII**: MongoDB schema documented in `data-model.md` with indexes and constraints (N/A - no collections created)

**Frontend Features** (`web/`):
- [x] **Principle V**: Feature resides in `src/features/[name]/` with no cross-feature imports (N/A - configuration only)
- [x] **Principle V**: Core ports/services abstract external dependencies (N/A - configuration only)
- [x] **Principle VI**: All styling uses Tailwind with Nord Theme colors (applies - theme configuration required)
- [x] **Principle VII**: Component tests use React Testing Library (N/A - configuration only)
- [x] **Principle VIII**: TypeScript types aligned with backend API contracts (N/A - no API contracts yet)

**Both**:
- [x] No `[NEEDS CLARIFICATION]` markers in Technical Context
- [x] Complexity justifications documented if constitution principles violated (N/A - no violations)

**Notes**: This is an infrastructure/configuration feature with no domain logic or UI components. Most constitution principles are not applicable (N/A) as they govern application code structure. Principle VI applies to theme configuration which will be implemented according to Nord theme requirements.

## Project Structure

### Documentation (this feature)

```text
specs/001-environment-setup/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output - technology decisions and best practices
├── data-model.md        # Phase 1 output - N/A (no data modeling in this feature)
├── quickstart.md        # Phase 1 output - developer onboarding guide
├── contracts/           # Phase 1 output - N/A (no API contracts in this feature)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend Configuration (Java/Spring Boot - api/)
api/
├── src/main/
│   ├── java/com/mithrilvault/api/
│   │   ├── ApiApplication.java          # Main application class (exists)
│   │   ├── domain/config/
│   │   │   └── MapperConfig.java        # MapStruct config (exists)
│   │   ├── application/
│   │   │   └── GlobalExceptionHandler.java  # Global error handling (exists)
│   │   └── infrastructure/config/
│   │       ├── MongoConfig.java         # MongoDB reactive configuration (new)
│   │       ├── CorsConfig.java          # CORS configuration (new)
│   │       └── WebFluxConfig.java       # WebFlux configuration (new)
│   └── resources/
│       ├── application.yaml             # Main configuration (update)
│       ├── application-dev.yaml         # Development profile (new)
│       ├── application-test.yaml        # Test profile (new)
│       └── application-prod.yaml        # Production profile (new)
├── build.gradle                         # Dependencies (update)
└── .env.example                         # Environment variables template (new)

# Frontend Configuration (Next.js/React - web/)
web/
├── src/
│   ├── app/
│   │   ├── layout.tsx                   # Root layout (update for theme)
│   │   ├── globals.css                  # Global styles (update for theme)
│   │   └── page.tsx                     # Home page (exists)
│   ├── config/
│   │   ├── theme.ts                     # Nord theme configuration (new)
│   │   └── api.ts                       # API client configuration (new)
│   ├── core/                            # Create FSD structure
│   │   ├── contexts/                    # (directory placeholder)
│   │   ├── ports/                       # (directory placeholder)
│   │   └── services/                    # (directory placeholder)
│   ├── features/                        # Create FSD structure
│   │   └── .gitkeep                     # (placeholder)
│   └── shared/                          # Create FSD structure
│       ├── components/                  # (directory placeholder)
│       └── utils/                       # (directory placeholder)
├── components.json                      # Shadcn configuration (new)
├── tailwind.config.ts                   # Tailwind configuration (new)
├── postcss.config.mjs                   # PostCSS configuration (exists)
├── next.config.ts                       # Next.js configuration (update)
├── eslint.config.mjs                    # ESLint configuration (exists, update)
├── package.json                         # Dependencies (update)
└── .env.example                         # Environment variables template (new)

# Root Configuration
/
├── docker-compose.yml                   # MongoDB service (update)
├── .env.example                         # Shared environment variables (new)
└── README.md                            # Setup instructions (update)
```

**Structure Decision**: This is a full-stack configuration feature affecting both `api/` and `web/` directories. The structure follows the established hexagonal architecture for backend (configuration in `infrastructure/config/`) and prepares Feature-Sliced Design directories for frontend. Configuration files are organized by Spring profiles (backend) and Next.js conventions (frontend).

## Complexity Tracking

> **N/A** - No constitution violations requiring justification. This is a configuration-only feature that establishes the foundation for future application development. All architecture patterns will be enforced in subsequent features that implement business logic.
