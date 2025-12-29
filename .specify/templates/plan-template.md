# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [single/web/mobile - determines source structure]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Backend Features** (`api/`):
- [ ] **Principle I**: Domain layer has zero dependencies on Spring/MongoDB
- [ ] **Principle II**: Write operations modeled as Commands with dedicated CommandHandlers
- [ ] **Principle III**: Domain models use Records or Lombok @Value (immutable)
- [ ] **Principle IV**: MapStruct mappers isolate DTO ↔ Entity conversions
- [ ] **Principle VII**: Domain tests are pure unit tests (no Spring context)
- [ ] **Principle VIII**: OpenAPI contract defined in `/specs/[###-feature]/contracts/` before implementation
- [ ] **Principle VIII**: MongoDB schema documented in `data-model.md` with indexes and constraints

**Frontend Features** (`web/`):
- [ ] **Principle V**: Feature resides in `src/features/[name]/` with no cross-feature imports
- [ ] **Principle V**: Core ports/services abstract external dependencies
- [ ] **Principle VI**: All styling uses Tailwind with Nord Theme colors
- [ ] **Principle VII**: Component tests use React Testing Library
- [ ] **Principle VIII**: TypeScript types aligned with backend API contracts

**Both**:
- [ ] No `[NEEDS CLARIFICATION]` markers in Technical Context
- [ ] Complexity justifications documented if constitution principles violated

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., com/mithrilvault/api/domain/model, features/accounts). 
  The delivered plan must not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Backend Feature (Java/Spring Boot - api/)
api/src/main/java/com/mithrilvault/api/
├── domain/
│   ├── model/          # Domain entities (Records or @Value)
│   ├── ports/          # Repository/Service interfaces
│   ├── command/        # Command objects
│   ├── commandhandler/ # Command handlers
│   ├── exception/      # Domain exceptions
│   └── mapper/         # Domain ↔ DTO mappers
├── application/
│   ├── controllers/    # REST endpoints
│   ├── dto/            # Request/Response DTOs
│   └── mapper/         # DTO ↔ Command mappers (if needed)
└── infrastructure/
    ├── persistence/    # MongoDB repositories
    ├── config/         # Spring/MongoDB config
    └── mapper/         # Entity ↔ Domain mappers

api/src/test/java/com/mithrilvault/api/
├── domain/            # Pure unit tests (no Spring)
├── application/       # Integration tests (@WebFluxTest)
└── infrastructure/    # Integration tests (Testcontainers)

# [REMOVE IF UNUSED] Option 2: Frontend Feature (Next.js/React - web/)
web/src/
├── features/[feature-name]/
│   ├── components/    # Feature components
│   ├── hooks/         # Feature hooks
│   ├── types.ts       # Feature types
│   ├── api.ts         # API client
│   └── index.tsx      # Feature entry
├── core/
│   ├── contexts/      # React contexts (if new global state)
│   ├── ports/         # Interfaces (if new external dependency)
│   └── services/      # Port implementations
└── shared/
    ├── components/    # Reusable UI (if new)
    └── utils/         # Utilities (if new)

web/src/app/[feature-name]/
└── page.tsx           # Route entry (delegates to features/)

web/src/__tests__/
└── features/[feature-name]/  # Component tests

# [REMOVE IF UNUSED] Option 3: Full-Stack Feature (api/ + web/)
api/
└── [same as Option 1 above]

web/
└── [same as Option 2 above]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
