# Research Document: Development Environment Setup

**Feature**: 001-environment-setup  
**Phase**: 0 (Outline & Research)  
**Date**: December 31, 2025

## Purpose

This document consolidates research findings and technology decisions for setting up the development environment for Mithril Vault. It resolves all technical unknowns and documents best practices for the chosen technology stack.

## Research Areas

### 1. Spring Boot Reactive Configuration Best Practices

**Decision**: Use Spring Boot 4.0.1 with WebFlux and Reactive MongoDB drivers

**Rationale**:
- Spring Boot 4.0.1 is the latest stable release supporting Java 21 LTS
- WebFlux provides non-blocking reactive streams compatible with high-concurrency financial operations
- Reactive MongoDB driver (spring-boot-starter-data-mongodb-reactive) ensures end-to-end reactive pipeline
- Aligns with constitution requirement for reactive patterns (no blocking I/O)

**Configuration Strategy**:
- Use YAML-based configuration for readability
- Separate profiles for dev/test/prod environments
- Connection pooling configured via `spring.data.mongodb.connection-pool.*`
- Health checks enabled via Spring Boot Actuator (`/actuator/health`)

**Alternatives Considered**:
- Spring MVC with blocking MongoDB driver: Rejected due to poor scalability for I/O-heavy financial operations
- Micronaut Framework: Rejected to maintain Spring ecosystem consistency (better tooling, documentation, team familiarity)

---

### 2. MongoDB Connection Configuration for Reactive Applications

**Decision**: Use connection URI with authentication and configure reactive connection pool settings

**Rationale**:
- MongoDB connection URI format supports authentication, replica sets, and connection options in a single string
- Reactive driver requires specific pool settings different from blocking driver (minSize, maxSize, maxWaitTime)
- Connection string format: `mongodb://username:password@host:port/database?authSource=admin`

**Configuration Parameters**:
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}  # Externalized to environment variable
      connection-pool:
        min-size: 10       # Minimum connections to maintain
        max-size: 100      # Maximum concurrent connections
        max-wait-time: 5000ms  # Timeout for acquiring connection
        max-connection-life-time: 30m  # Connection lifetime
        max-connection-idle-time: 10m  # Idle connection timeout
```

**Best Practices**:
- Use environment variables for sensitive data (never hardcode credentials)
- Enable connection pool monitoring via actuator metrics
- Configure appropriate timeouts to prevent resource exhaustion
- Use separate databases for dev/test/prod (never share)

**Alternatives Considered**:
- Individual property-based configuration: Rejected in favor of URI format for better portability
- Blocking driver with thread pools: Rejected due to reactive architecture requirement

---

### 3. CORS Configuration for Next.js Frontend

**Decision**: Implement CORS configuration bean with environment-specific allowed origins

**Rationale**:
- Next.js dev server runs on `http://localhost:3000` by default
- Production frontend will be served from different origin (TBD deployment)
- CORS must be explicitly configured in Spring WebFlux via `CorsConfiguration`
- Allow credentials required for cookie-based authentication (future)

**Configuration Strategy**:
```java
@Configuration
public class CorsConfig implements WebFluxConfigurer {
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

**Environment-Specific Origins**:
- Development: `http://localhost:3000,http://127.0.0.1:3000`
- Production: `${FRONTEND_URL}` (environment variable)

**Alternatives Considered**:
- Global CORS with wildcard (`*`): Rejected due to security concerns
- No CORS (proxy setup): Rejected to maintain separate deployability of frontend/backend

---

### 4. Next.js 16 App Router Configuration

**Decision**: Use App Router with TypeScript, Server Components by default, and Client Components only when needed

**Rationale**:
- App Router (stable since Next.js 13) provides improved performance and developer experience
- Server Components reduce client-side JavaScript bundle size
- Built-in support for streaming, suspense, and progressive enhancement
- Better integration with React 19 features

**Configuration in next.config.ts**:
```typescript
const nextConfig: NextConfig = {
  reactStrictMode: true,
  experimental: {
    optimizePackageImports: ['lucide-react'],  // Optimize icon imports
  },
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  },
};
```

**Best Practices**:
- Use Server Components by default (no "use client" directive)
- Add "use client" only for interactive components (forms, modals, charts)
- Leverage `loading.tsx` and `error.tsx` for better UX
- Use `layout.tsx` for shared layouts and metadata

**Alternatives Considered**:
- Pages Router: Rejected as App Router is the recommended approach for new projects
- Client-side rendering only: Rejected due to worse performance and SEO

---

### 5. Tailwind CSS 4.x Configuration with Nord Theme

**Decision**: Use Tailwind CSS 4.x (latest) with custom Nord theme colors extended in configuration

**Rationale**:
- Tailwind CSS 4.x provides new CSS-first configuration via `@import` directives
- Nord theme provides aesthetically pleasing, accessible color palette for financial applications
- Utility-first approach reduces CSS bundle size and prevents style conflicts
- Easy integration with shadcn/ui components

**Nord Theme Color Mapping**:
```typescript
// Nord Polar Night (backgrounds, dark surfaces)
nord0: '#2E3440',   // Darkest - primary background
nord1: '#3B4252',   // Dark - elevated surfaces
nord2: '#434C5E',   // Medium - borders, dividers
nord3: '#4C566A',   // Light - disabled states

// Nord Snow Storm (text, light surfaces)
nord4: '#D8DEE9',   // Light text on dark
nord5: '#E5E9F0',   // Primary text
nord6: '#ECEFF4',   // Brightest - white backgrounds

// Nord Frost (primary actions, links, highlights)
nord7: '#8FBCBB',   // Teal - secondary actions
nord8: '#88C0D0',   // Cyan - primary actions
nord9: '#81A1C1',   // Blue - links
nord10: '#5E81AC',  // Dark blue - visited links

// Nord Aurora (semantic colors)
nord11: '#BF616A',  // Red - errors, destructive actions
nord12: '#D08770',  // Orange - warnings
nord13: '#EBCB8B',  // Yellow - caution
nord14: '#A3BE8C',  // Green - success
nord15: '#B48EAD',  // Purple - special highlights
```

**Configuration Strategy**:
- Extend Tailwind's default theme (don't replace)
- Define custom colors in `tailwind.config.ts`
- Create semantic aliases (primary, success, error, warning)
- Use CSS variables for dynamic theme switching (future enhancement)

**Alternatives Considered**:
- Material Design colors: Rejected in favor of Nord for cohesive brand identity
- Custom CSS: Rejected in favor of Tailwind utilities for consistency
- Tailwind CSS 3.x: Rejected as v4 is stable and provides better performance

---

### 6. Shadcn UI Integration Strategy

**Decision**: Install shadcn/ui CLI and add components on-demand using CLI, with custom Nord theme overrides

**Rationale**:
- Shadcn/ui provides unstyled Radix UI primitives with Tailwind styling
- Components are copied into project (not npm dependency) allowing full customization
- Excellent accessibility and keyboard navigation out-of-the-box
- Aligns with constitution requirement for Tailwind-based UI

**Integration Steps**:
1. Initialize shadcn: `npx shadcn@latest init`
2. Configure `components.json` with Nord theme colors
3. Add components as needed: `npx shadcn@latest add button card`
4. Components installed to `src/shared/components/ui/`

**Configuration (components.json)**:
```json
{
  "style": "new-york",
  "tailwind": {
    "config": "tailwind.config.ts",
    "css": "src/app/globals.css",
    "baseColor": "neutral",
    "cssVariables": true
  },
  "aliases": {
    "components": "@/shared/components",
    "utils": "@/shared/utils"
  }
}
```

**Alternatives Considered**:
- Material-UI (MUI): Rejected due to CSS-in-JS approach conflicting with Tailwind
- Ant Design: Rejected due to opinionated styling difficult to override
- Headless UI: Considered but shadcn provides better DX with CLI and examples

---

### 7. ESLint Configuration for Next.js + TypeScript

**Decision**: Use Next.js recommended ESLint config with TypeScript-specific rules

**Rationale**:
- `eslint-config-next` includes React, React Hooks, and Next.js-specific rules
- Enforces Next.js best practices (no img tag, proper Link usage)
- TypeScript ESLint parser catches type-related issues
- Integrates with VS Code for real-time feedback

**Configuration in eslint.config.mjs**:
```javascript
import { dirname } from "path";
import { fileURLToPath } from "url";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const compat = new FlatCompat({
  baseDirectory: __dirname,
});

const eslintConfig = [
  ...compat.extends("next/core-web-vitals", "next/typescript"),
  {
    rules: {
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
      "@typescript-eslint/no-explicit-any": "warn",
    },
  },
];

export default eslintConfig;
```

**Best Practices**:
- Run ESLint on pre-commit hook (future: husky + lint-staged)
- Configure VS Code to auto-fix on save
- Enforce strict TypeScript mode in tsconfig.json
- Add custom rules for FSD architecture (future: custom ESLint plugin)

**Alternatives Considered**:
- Airbnb config: Rejected as too opinionated for Next.js projects
- Standard JS: Rejected due to lack of TypeScript support
- No linting: Rejected to maintain code quality

---

### 8. Spring Boot Actuator Configuration

**Decision**: Enable Spring Boot Actuator with health and info endpoints exposed

**Rationale**:
- Provides production-ready monitoring and management endpoints
- Health checks essential for container orchestration (future Kubernetes deployment)
- Metrics integration with Micrometer (future Prometheus monitoring)
- Info endpoint for version and build information

**Configuration**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized  # Hide details in production
  health:
    mongo:
      enabled: true  # Include MongoDB health check
```

**Health Check Response**:
```json
{
  "status": "UP",
  "components": {
    "mongo": {
      "status": "UP",
      "details": { "version": "7.0.5" }
    },
    "diskSpace": {
      "status": "UP",
      "details": { "free": 10737418240 }
    }
  }
}
```

**Alternatives Considered**:
- Custom health check endpoints: Rejected in favor of standard Actuator
- Full actuator exposure: Rejected due to security concerns (only expose essential endpoints)

---

### 9. Environment Variable Management Strategy

**Decision**: Use `.env` files for local development with `.env.example` templates, and system environment variables for deployment

**Rationale**:
- `.env` files provide easy local configuration without modifying code
- `.env.example` documents all required variables without exposing secrets
- Spring Boot automatically loads `.env` files in development
- Next.js supports `.env.local` for local overrides
- Production uses system environment variables (container secrets, CI/CD variables)

**File Structure**:
```
/
├── .env.example          # Template with dummy values
├── .env                  # Local overrides (gitignored)
api/
├── .env.example          # Backend-specific variables
└── .env                  # (gitignored)
web/
├── .env.example          # Frontend-specific variables
└── .env.local            # (gitignored)
```

**Variable Naming Conventions**:
- Backend: `MONGODB_URI`, `CORS_ALLOWED_ORIGINS`, `JWT_SECRET`
- Frontend: `NEXT_PUBLIC_API_URL` (prefix required for browser access)
- Shared: `NODE_ENV`, `PORT`, `LOG_LEVEL`

**Security Best Practices**:
- Never commit actual `.env` files (add to .gitignore)
- Use strong, unique secrets generated via `openssl rand -base64 32`
- Rotate secrets regularly in production
- Use secret management tools in production (AWS Secrets Manager, Vault)

**Alternatives Considered**:
- Hardcoded configuration: Rejected due to security and portability concerns
- Config files (application-local.yaml): Rejected to avoid accidental commits

---

### 10. Docker Compose Configuration for Local Development

**Decision**: Use docker-compose.yml with MongoDB service and volume persistence

**Rationale**:
- Provides consistent development environment across team members
- Eliminates "works on my machine" issues
- MongoDB runs in container (no local installation required)
- Data persisted in named volumes (survives container restarts)

**Configuration Enhancement**:
```yaml
services:
  mongodb:
    image: mongo:7.0
    container_name: mithril_mongodb
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGODB_ROOT_USER:-root}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGODB_ROOT_PASSWORD:-root}
      MONGO_INITDB_DATABASE: ${MONGODB_DATABASE:-mithril_vault}
    ports:
      - "${MONGODB_PORT:-27017}:27017"
    volumes:
      - mongodb_data:/data/db
      - mongodb_config:/data/configdb
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 40s

volumes:
  mongodb_data:
    driver: local
  mongodb_config:
    driver: local
```

**Usage**:
- Start: `docker-compose up -d`
- Stop: `docker-compose down`
- Reset data: `docker-compose down -v` (removes volumes)
- View logs: `docker-compose logs -f mongodb`

**Alternatives Considered**:
- Local MongoDB installation: Rejected to reduce onboarding complexity
- Cloud MongoDB (Atlas): Rejected for local development (added latency, requires internet)
- Testcontainers only: Rejected as it doesn't provide persistent local database

---

## Implementation Readiness

All research complete. No `[NEEDS CLARIFICATION]` markers remain. Ready to proceed to Phase 1 (Design & Contracts).

### Key Decisions Summary

| Area | Decision | Configuration File |
|------|----------|-------------------|
| Backend Framework | Spring Boot 4.0.1 + WebFlux | build.gradle |
| Database | MongoDB (Reactive Driver) | application.yaml |
| CORS | Environment-specific origins | CorsConfig.java |
| Frontend Framework | Next.js 16.1.1 + App Router | next.config.ts |
| Styling | Tailwind CSS 4.x + Nord Theme | tailwind.config.ts |
| UI Components | Shadcn/ui (Radix + Tailwind) | components.json |
| Linting | ESLint (Next.js config) | eslint.config.mjs |
| Monitoring | Spring Boot Actuator | application.yaml |
| Secrets | .env files + env variables | .env.example |
| Local Infra | Docker Compose (MongoDB) | docker-compose.yml |

### Dependencies to Add

**Backend (build.gradle)**:
- `spring-boot-starter-data-mongodb-reactive`
- `spring-boot-starter-actuator`
- `spring-boot-starter-webflux` (likely already included)

**Frontend (package.json)**:
- `@radix-ui/*` (via shadcn CLI)
- `class-variance-authority` (via shadcn CLI)
- `clsx` (via shadcn CLI)
- `tailwind-merge` (via shadcn CLI)

**Note**: Using pnpm as package manager for better performance and disk space efficiency.

### Configuration Files to Create/Update

**Create**:
- `api/src/main/resources/application-dev.yaml`
- `api/src/main/resources/application-test.yaml`
- `api/src/main/resources/application-prod.yaml`
- `api/src/main/java/com/mithrilvault/api/infrastructure/config/MongoConfig.java`
- `api/src/main/java/com/mithrilvault/api/infrastructure/config/CorsConfig.java`
- `api/src/main/java/com/mithrilvault/api/infrastructure/config/WebFluxConfig.java`
- `web/src/config/theme.ts`
- `web/src/config/api.ts`
- `web/components.json`
- `web/tailwind.config.ts`
- `.env.example` (root, api/, web/)

**Update**:
- `api/src/main/resources/application.yaml`
- `api/build.gradle`
- `web/src/app/layout.tsx`
- `web/src/app/globals.css`
- `web/next.config.ts`
- `web/eslint.config.mjs`
- `web/package.json`
- `docker-compose.yml`
- `README.md`
