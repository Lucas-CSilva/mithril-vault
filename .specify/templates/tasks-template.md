---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: The examples below include test tasks. Tests are OPTIONAL - only include them if explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend (api/)**: `api/src/main/java/com/mithrilvault/api/[layer]/[package]/`, `api/src/test/java/com/mithrilvault/api/[layer]/`
- **Frontend (web/)**: `web/src/features/[feature]/`, `web/src/core/`, `web/src/shared/`, `web/src/__tests__/`
- **Full-Stack**: Combine both backend and frontend paths
- Paths shown below assume backend-only - adjust based on plan.md structure

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per implementation plan
- [ ] T002 Initialize [language] project with [framework] dependencies
- [ ] T003 [P] Configure linting and formatting tools

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

**Schema Definition** (MANDATORY - Principle VIII):

- [ ] T004 Define OpenAPI 3.0+ contract for [resource] in /specs/[###-feature]/contracts/[resource].openapi.yaml
- [ ] T005 Document MongoDB schema in /specs/[###-feature]/data-model.md (collections, fields, indexes, constraints)
- [ ] T006 [P] Generate TypeScript types from OpenAPI contract for frontend

**Backend Infrastructure**:

- [ ] T007 Setup MongoDB repository base interfaces in api/.../domain/ports/
- [ ] T008 [P] Add ArchUnit tests to enforce layer boundaries (domain cannot import application/infrastructure)
- [ ] T009 [P] Configure MapStruct processor in api/build.gradle
- [ ] T010 Create domain exception hierarchy in api/.../domain/exception/
- [ ] T011 Configure Spring WebFlux router and global exception handler in api/.../infrastructure/config/
- [ ] T012 Setup Testcontainers for MongoDB integration tests

**Frontend Infrastructure**:

- [ ] T013 Create base API client port in web/src/core/ports/IApiClient.ts
- [ ] T014 [P] Implement API client service in web/src/core/services/ApiClient.ts
- [ ] T015 [P] Setup React Query provider in web/src/app/layout.tsx
- [ ] T016 Create auth context in web/src/core/contexts/AuthContext.tsx
- [ ] T017 Add Nord Theme Tailwind config to web/tailwind.config.ts

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (OPTIONAL - only if tests requested) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

**Contract Tests** (verify implementation matches OpenAPI spec):

- [ ] T018 [P] [US1] Contract test for [endpoint] verifies OpenAPI compliance in api/.../application/controllers/[Name]ControllerContractTest.java

**Backend Tests**:

- [ ] T019 [P] [US1] Pure unit test for [CommandHandler] in api/.../domain/commandhandler/[Name]CommandHandlerTest.java (NO Spring context)
- [ ] T020 [P] [US1] Integration test for [Repository] in api/.../infrastructure/persistence/[Name]RepositoryTest.java (Testcontainers)
- [ ] T021 [P] [US1] API integration test for [endpoint] in api/.../application/controllers/[Name]ControllerTest.java (@WebFluxTest)

**Frontend Tests**:

- [ ] T022 [P] [US1] Component test for [Component] in web/src/__tests__/features/[feature]/[Component].test.tsx (React Testing Library)

### Implementation for User Story 1

**Backend** (CQRS + Hexagonal + Schema-First):

- [ ] T023 [P] [US1] Create [Entity] domain model (Record) in api/.../domain/model/[Entity].java (based on data-model.md schema)
- [ ] T024 [P] [US1] Create [Command] object (Record) in api/.../domain/command/[Action][Entity]Command.java
- [ ] T025 [P] [US1] Define [Repository] port interface in api/.../domain/ports/[Entity]Repository.java
- [ ] T026 [US1] Implement [CommandHandler] in api/.../domain/commandhandler/[Action][Entity]CommandHandler.java (depends on T023, T024, T025)
- [ ] T027 [US1] Implement [Repository] in api/.../infrastructure/persistence/[Entity]RepositoryImpl.java (depends on T025)
- [ ] T028 [US1] Create MapStruct mapper in api/.../domain/mapper/[Entity]Mapper.java
- [ ] T029 [US1] Create [Request/Response]DTO (Records) in api/.../application/dto/[Entity][Request|Response].java (matches OpenAPI contract)
- [ ] T030 [US1] Implement REST controller in api/.../application/controllers/[Entity]Controller.java (implements OpenAPI contract, depends on T026, T028, T029)

**Frontend** (FSD + Hexagonal + Schema-First):

- [ ] T031 [P] [US1] Define feature types in web/src/features/[feature]/types.ts (generated from OpenAPI or manually aligned)
- [ ] T032 [P] [US1] Create API client functions in web/src/features/[feature]/api.ts (uses core/ports, matches OpenAPI contract)
- [ ] T033 [P] [US1] Create [Hook] in web/src/features/[feature]/hooks/use[Name].ts (React Query)
- [ ] T034 [US1] Implement [Component] in web/src/features/[feature]/components/[Component].tsx (depends on T033)
- [ ] T035 [US1] Create feature entry in web/src/features/[feature]/index.tsx
- [ ] T036 [US1] Add route page in web/src/app/[feature]/page.tsx (delegates to T035)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (OPTIONAL - only if tests requested) ⚠️

- [ ] T018 [P] [US2] Contract test for [endpoint] in tests/contract/test_[name].py
- [ ] T019 [P] [US2] Integration test for [user journey] in tests/integration/test_[name].py

### Implementation for User Story 2

- [ ] T020 [P] [US2] Create [Entity] model in src/models/[entity].py
- [ ] T021 [US2] Implement [Service] in src/services/[service].py
- [ ] T022 [US2] Implement [endpoint/feature] in src/[location]/[file].py
- [ ] T023 [US2] Integrate with User Story 1 components (if needed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (OPTIONAL - only if tests requested) ⚠️

- [ ] T024 [P] [US3] Contract test for [endpoint] in tests/contract/test_[name].py
- [ ] T025 [P] [US3] Integration test for [user journey] in tests/integration/test_[name].py

### Implementation for User Story 3

- [ ] T026 [P] [US3] Create [Entity] model in src/models/[entity].py
- [ ] T027 [US3] Implement [Service] in src/services/[service].py
- [ ] T028 [US3] Implement [endpoint/feature] in src/[location]/[file].py

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Code cleanup and refactoring
- [ ] TXXX Performance optimization across all stories
- [ ] TXXX [P] Additional unit tests (if requested) in tests/unit/
- [ ] TXXX Security hardening
- [ ] TXXX Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (if tests requested):
Task: "Contract test for [endpoint] in tests/contract/test_[name].py"
Task: "Integration test for [user journey] in tests/integration/test_[name].py"

# Launch all models for User Story 1 together:
Task: "Create [Entity1] model in src/models/[entity1].py"
Task: "Create [Entity2] model in src/models/[entity2].py"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
