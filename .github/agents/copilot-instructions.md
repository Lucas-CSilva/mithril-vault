# mithril-vault Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-12-31

## Active Technologies

### Backend (api/)
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 4.0.1 with WebFlux (reactive)
- **Database**: MongoDB (Reactive Driver via Spring Data MongoDB Reactive)
- **Build Tool**: Gradle 8.x
- **Key Libraries**: MapStruct 1.6.3, Lombok, Spring Boot Actuator
- **Testing**: JUnit 5, Spring Boot Test, Testcontainers
- **Code Quality**: Spotless (Google Java Format)
- (001-environment-setup)

### Frontend (web/)
- **Language**: TypeScript 5.x
- **Framework**: Next.js 16.1.1 with App Router, React 19.2.3
- **Styling**: Tailwind CSS 4.x with Nord theme palette
- **UI Components**: Shadcn/ui (Radix UI primitives)
- **Package Manager**: pnpm
- **Runtime**: Node.js 20.x (LTS)
- **Testing**: React Testing Library, Jest
- **Linting**: ESLint (Next.js config)
- (001-environment-setup)

### Infrastructure
- **Local Development**: Docker Compose with MongoDB 7.0
- **Configuration**: Environment variables (.env files)
- (001-environment-setup)

## Project Structure

```text
api/                          # Backend (Java/Spring Boot)
????????? src/main/java/.../api/
???   ????????? domain/              # Business logic (pure, no framework deps)
???   ????????? application/         # REST controllers (HTTP layer)
???   ????????? infrastructure/      # DB, configs, external integrations
????????? src/main/resources/
???   ????????? application*.yaml    # Spring configuration (dev/test/prod profiles)
????????? build.gradle

web/                          # Frontend (Next.js/React/TypeScript)
????????? src/
???   ????????? app/                 # Next.js routing (App Router)
???   ????????? features/            # Business features (Feature-Sliced Design)
???   ????????? core/                # Dependency injection, services, ports
???   ????????? shared/              # Reusable UI components (Shadcn/ui)
???   ????????? config/              # Theme, API client configs
????????? package.json
????????? next.config.ts

specs/                        # Feature specifications and implementation plans
docker-compose.yml            # Local infrastructure (MongoDB)
```

## Commands

### Backend Development
```bash
cd api
./gradlew bootRun              # Run backend (localhost:8080)
./gradlew test                 # Run tests
./gradlew spotlessApply        # Format code
./gradlew build                # Build JAR
```

### Frontend Development
```bash
cd web
pnpm install                   # Install dependencies
pnpm dev                       # Run dev server (localhost:3000)
pnpm build                     # Build production bundle
pnpm lint                      # Lint code
pnpm test                      # Run tests
```

### Infrastructure
```bash
docker-compose up -d           # Start MongoDB
docker-compose down            # Stop services
docker-compose logs -f mongodb # View logs
```

## Code Style

### Backend
- **Architecture**: Hexagonal (Ports & Adapters)
- **Pattern**: CQRS (Command Query Responsibility Segregation)
- **Immutability**: Use Records or Lombok @Value
- **Mapping**: MapStruct for DTO ??? Entity conversions
- **Formatting**: Google Java Format (via Spotless)
- **Testing**: Pure unit tests for domain, integration tests for infrastructure

### Frontend
- **Architecture**: Feature-Sliced Design (FSD)
- **Styling**: Tailwind CSS utilities only (no custom CSS)
- **Theme**: Nord color palette
- **Components**: Shadcn/ui for UI primitives
- **TypeScript**: Strict mode enabled
- **Formatting**: Prettier (via Next.js)
- **Linting**: ESLint (Next.js + TypeScript rules)

See `.specify/memory/constitution.md` for complete architecture rules.

## Recent Changes

- 001-environment-setup: Added development environment configuration

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
