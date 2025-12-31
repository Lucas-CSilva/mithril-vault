# Tasks: Development Environment Setup

**Input**: Design documents from `/specs/001-environment-setup/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md (N/A), contracts/ (N/A)

**Tests**: Not requested - No test tasks included

**Organization**: Tasks are grouped by user story to enable independent implementation and validation of frontend and backend setup.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend (api/)**: `api/src/main/java/com/mithrilvault/api/[layer]/[package]/`, `api/src/main/resources/`
- **Frontend (web/)**: `web/src/app/`, `web/src/config/`, `web/src/features/`, `web/src/core/`, `web/src/shared/`
- **Root**: `/docker-compose.yml`, `/.env.example`, `/README.md`

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Prepare repository structure and documentation templates

- [X] T001 Create root environment variables template file at .env.example with MongoDB settings
- [X] T002 [P] Create backend environment variables template at api/.env.example with MongoDB URI and CORS origins
- [X] T003 [P] Create frontend environment variables template at web/.env.example with API URL
- [X] T004 Update root README.md with quick start instructions and project structure overview

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

**Docker Infrastructure**:

- [X] T005 Update docker-compose.yml with MongoDB service configuration including health checks and volume persistence

**Directory Structure**:

- [X] T006 [P] Create frontend FSD directory structure: web/src/features/, web/src/core/contexts/, web/src/core/ports/, web/src/core/services/, web/src/shared/components/, web/src/shared/utils/
- [X] T007 [P] Add .gitkeep files to empty directories created in T006

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Frontend Developer Onboarding (Priority: P1) 🎯 MVP

**Goal**: Configure all frontend tooling (Next.js, Tailwind, ESLint, Shadcn) and establish Nord theme design system so developers can start building UI components immediately

**Independent Test**: New developer runs `pnpm install && pnpm dev`, creates a sample component using theme colors and Shadcn UI components, verifies hot-reload works, and ESLint catches style issues

### Implementation for User Story 1

**Next.js Configuration**:

- [X] T008 [US1] Update web/next.config.ts to enable React strict mode and optimize package imports for lucide-react
- [X] T009 [US1] Update web/package.json to add shadcn/ui dependencies (class-variance-authority, clsx, tailwind-merge) and Radix UI primitives

**Tailwind & Theme Configuration**:

- [X] T010 [US1] Create web/src/config/theme.ts with complete Nord theme color definitions (polarNight, snowStorm, frost, aurora) and semantic color mappings
- [X] T011 [US1] Create web/tailwind.config.ts extending default theme with Nord colors from theme.ts and configuring Shadcn component paths
- [X] T012 [US1] Update web/src/app/globals.css to include Tailwind directives and CSS variables for Nord theme colors aligned with Shadcn theming system
- [X] T013 [US1] Update web/src/app/layout.tsx to apply Nord theme colors to root HTML element and configure metadata

**ESLint Configuration**:

- [X] T014 [US1] Update web/eslint.config.mjs to add TypeScript-specific rules (no-unused-vars with underscore exception, no-explicit-any as warning)

**Shadcn UI Integration**:

- [X] T015 [US1] Create web/components.json configuring Shadcn with New York style, Nord theme base color, CSS variables mode, and FSD-aligned component paths
- [X] T016 [US1] Install initial Shadcn components (button, card) to web/src/shared/components/ui/ using npx shadcn@latest add command

**API Client Setup**:

- [X] T017 [US1] Create web/src/config/api.ts with API base URL configuration from NEXT_PUBLIC_API_URL environment variable

**Checkpoint**: At this point, User Story 1 should be fully functional - developer can start frontend, use theme colors, import Shadcn components, and ESLint works correctly

---

## Phase 4: User Story 2 - Backend Developer Onboarding (Priority: P1)

**Goal**: Configure Spring Boot with reactive MongoDB connection, health checks, CORS, and environment-specific profiles so backend developers can start building API endpoints immediately

**Independent Test**: New developer starts API service with `./gradlew bootRun`, verifies MongoDB connection succeeds, accesses `/actuator/health` endpoint showing UP status, and confirms application logs show proper configuration loading

### Implementation for User Story 2

**Gradle Dependencies**:

- [X] T018 [US2] Update api/build.gradle to add spring-boot-starter-data-mongodb-reactive and spring-boot-starter-actuator dependencies

**MongoDB Configuration**:

- [X] T019 [P] [US2] Create api/src/main/java/com/mithrilvault/api/infrastructure/config/MongoConfig.java with reactive MongoDB configuration and connection pool settings
- [X] T020 [P] [US2] Update api/src/main/resources/application.yaml with MongoDB connection URI from environment variable and connection pool parameters (min-size: 10, max-size: 100, timeouts)

**Spring Profiles**:

- [X] T021 [P] [US2] Create api/src/main/resources/application-dev.yaml with development-specific settings (verbose logging, show SQL, relaxed security)
- [X] T022 [P] [US2] Create api/src/main/resources/application-test.yaml with test-specific settings (embedded test database preference, test data initialization)
- [X] T023 [P] [US2] Create api/src/main/resources/application-prod.yaml with production-specific settings (minimal logging, security hardening, performance optimization)

**CORS Configuration**:

- [X] T024 [US2] Create api/src/main/java/com/mithrilvault/api/infrastructure/config/CorsConfig.java implementing WebFluxConfigurer with environment-specific allowed origins from CORS_ALLOWED_ORIGINS

**WebFlux Configuration**:

- [X] T025 [US2] Create api/src/main/java/com/mithrilvault/api/infrastructure/config/WebFluxConfig.java with codec configuration and reactive context propagation settings

**Actuator Configuration**:

- [X] T026 [US2] Update api/src/main/resources/application.yaml to enable Spring Boot Actuator health and info endpoints with MongoDB health indicator and show-details setting

**Checkpoint**: At this point, User Story 2 should be fully functional - backend starts successfully, connects to MongoDB, health checks return UP status, and CORS is configured for frontend

---

## Phase 5: User Story 3 - Full-Stack Developer Local Development (Priority: P2)

**Goal**: Enable full-stack development with frontend and backend communicating correctly, proper CORS configuration, and docker-compose orchestration of all services

**Independent Test**: Developer runs `docker-compose up -d` to start MongoDB, starts backend with `./gradlew bootRun`, starts frontend with `pnpm dev`, and verifies frontend can make a successful request to backend health endpoint without CORS errors

### Implementation for User Story 3

**Environment Integration**:

- [X] T027 [US3] Document MongoDB connection URI format in api/.env.example with authentication and connection options for docker-compose MongoDB service
- [X] T028 [US3] Document CORS allowed origins in api/.env.example including both localhost:3000 and 127.0.0.1:3000 for development
- [X] T029 [US3] Document frontend API URL in web/.env.example pointing to backend running on localhost:8080

**Docker Compose Enhancement**:

- [X] T030 [US3] Verify docker-compose.yml MongoDB configuration matches environment variable references in .env.example and includes proper volume mounting for data persistence

**Cross-Service Validation**:

- [X] T031 [US3] Update root README.md with full-stack startup sequence (docker-compose → backend → frontend) and troubleshooting guide for common issues

**Checkpoint**: All user stories should now be independently functional - developers can run entire stack locally with proper frontend-backend communication

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements and validation affecting multiple components

- [X] T032 [P] Add inline comments to all configuration files (application.yaml, next.config.ts, tailwind.config.ts) explaining key settings per success criteria SC-007
- [X] T033 [P] Validate all environment variable files (.env.example) are properly documented with descriptions and example values per functional requirement FR-016
- [X] T034 [P] Add graceful error handling in api/src/main/java/com/mithrilvault/api/application/GlobalExceptionHandler.java for missing environment variables with clear error messages per success criteria SC-008
- [X] T035 Run complete onboarding validation following specs/001-environment-setup/quickstart.md to verify 15-minute setup goal per success criteria SC-001
- [X] T036 [P] Verify frontend hot-reload performance is under 2 seconds per success criteria SC-002
- [X] T037 [P] Verify backend startup time is under 30 seconds with database connection per success criteria SC-003
- [X] T038 Run ESLint on entire web/ codebase and verify zero errors per success criteria SC-004

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3, 4, 5)**: All depend on Foundational phase completion
  - User Story 1 (Frontend) and User Story 2 (Backend) can proceed in parallel (if staffed)
  - User Story 3 (Integration) depends on User Story 1 AND User Story 2 completion
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1) - Frontend**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1) - Backend**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P2) - Integration**: Depends on both User Story 1 AND User Story 2 completion

### Within Each User Story

**User Story 1 (Frontend)**:
- T008-T009: Configuration files can be done together
- T010: Theme definition must come before T011 (Tailwind config needs theme.ts)
- T011-T014: Can be done in parallel after T010
- T015-T016: Shadcn setup sequential (components.json before installing components)
- T017: API client config can be done anytime in parallel

**User Story 2 (Backend)**:
- T018: Dependencies must be added before T019-T026
- T019-T023: All configuration files can be created in parallel after T018
- T024-T026: Can be done in parallel after T019-T023

**User Story 3 (Integration)**:
- T027-T029: All environment documentation can be done in parallel
- T030-T031: Documentation validation sequential

### Parallel Opportunities

- **Phase 1**: T002 and T003 can run in parallel
- **Phase 2**: T006 and T007 can run in parallel
- **User Story 1**: T011, T012, T013, T014, T017 can all run in parallel after T010 completes
- **User Story 2**: T019 and T020 can run together; T021, T022, T023 can all run in parallel; T024 and T025 can run in parallel
- **User Story 3**: T027, T028, T029 can all run in parallel
- **Phase 6 (Polish)**: T032, T033, T034, T036, T037, T038 can all run in parallel

---

## Parallel Example: User Story 1 (Frontend)

```bash
# After T010 (theme.ts) is complete, launch in parallel:
Task: "Update web/tailwind.config.ts with Nord colors"
Task: "Update web/src/app/globals.css with CSS variables"
Task: "Update web/src/app/layout.tsx with theme"
Task: "Update web/eslint.config.mjs with TypeScript rules"
Task: "Create web/src/config/api.ts with API configuration"
```

## Parallel Example: User Story 2 (Backend)

```bash
# After T018 (dependencies) is complete, launch in parallel:
Task: "Create MongoConfig.java"
Task: "Update application.yaml with MongoDB settings"

# Then launch profile configurations in parallel:
Task: "Create application-dev.yaml"
Task: "Create application-test.yaml"
Task: "Create application-prod.yaml"

# Then launch remaining configs in parallel:
Task: "Create CorsConfig.java"
Task: "Create WebFluxConfig.java"
```

---

## Implementation Strategy

### MVP First (Frontend + Backend Separately)

**Option A - Frontend First**:
1. Complete Phase 1: Setup → Phase 2: Foundational
2. Complete Phase 3: User Story 1 (Frontend)
3. **STOP and VALIDATE**: Test frontend in isolation, create sample components with theme
4. Proceed to Phase 4: User Story 2 (Backend)

**Option B - Backend First**:
1. Complete Phase 1: Setup → Phase 2: Foundational
2. Complete Phase 4: User Story 2 (Backend)
3. **STOP and VALIDATE**: Test backend in isolation, verify health checks and database connection
4. Proceed to Phase 3: User Story 1 (Frontend)

**Option C - Parallel Development** (recommended if 2+ developers):
1. Complete Phase 1: Setup → Phase 2: Foundational (together)
2. Developer A: Phase 3 User Story 1 (Frontend)
   Developer B: Phase 4 User Story 2 (Backend)
3. Both developers converge on Phase 5: User Story 3 (Integration)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 (Frontend) → Test independently → Demo frontend with mock data
3. Add User Story 2 (Backend) → Test independently → Demo health checks and MongoDB
4. Add User Story 3 (Integration) → Test end-to-end → Full stack running
5. Add Polish → Production-ready environment

### Full-Stack Validation Checkpoints

- After User Story 1: Frontend developer can build UI components with design system
- After User Story 2: Backend developer can build API endpoints with database access
- After User Story 3: Full-stack developer can develop features spanning both layers
- After Polish: New developers can complete onboarding in under 15 minutes

---

## Success Metrics

Upon completion of all tasks, the following success criteria from spec.md should be met:

- **SC-001**: ✅ New developer onboarding completes in under 15 minutes (validated by T035)
- **SC-002**: ✅ Frontend hot-reload completes within 2 seconds (validated by T036)
- **SC-003**: ✅ Backend startup with database connection completes within 30 seconds (validated by T037)
- **SC-004**: ✅ ESLint executes without errors (validated by T038)
- **SC-005**: ✅ Theme colors consistently applied via Nord theme configuration (User Story 1)
- **SC-006**: ✅ Frontend-backend communication works without CORS errors (User Story 3)
- **SC-007**: ✅ Configuration files documented with inline comments (T032)
- **SC-008**: ✅ Missing environment variables produce clear error messages (T034)

---

## Notes

- This is a configuration-only feature with no domain logic or API endpoints
- Tests are not included as they were not requested in the specification
- Each user story can be validated independently before proceeding to the next
- User Story 1 (Frontend) and User Story 2 (Backend) have no dependencies on each other
- User Story 3 (Integration) requires both User Story 1 and 2 to be complete
- All tasks include exact file paths for clarity
- [P] markers indicate parallelizable tasks within the same user story
- [Story] markers (US1, US2, US3) map tasks to their respective user stories for traceability
