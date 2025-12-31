# API Contracts: Development Environment Setup

**Feature**: 001-environment-setup  
**Phase**: 1 (Design & Contracts)  
**Date**: December 31, 2025

## Contract Status

**Status**: N/A - No API Contracts Required

**Rationale**: This is a pure infrastructure/configuration feature that does not introduce any API endpoints. The feature focuses on:

1. Establishing development environment configuration
2. Setting up build tools, linters, and styling frameworks
3. Configuring database connectivity
4. Creating directory structures for future development

No REST endpoints, GraphQL schemas, or WebSocket connections are introduced as part of this feature.

## Infrastructure Endpoints

While no business API endpoints are created, the following infrastructure endpoints will be available after configuration:

### Spring Boot Actuator Health Check

**Endpoint**: `GET /actuator/health`

**Purpose**: System health monitoring (not a business API)

**Response** (when healthy):
```json
{
  "status": "UP",
  "components": {
    "mongo": {
      "status": "UP",
      "details": {
        "version": "7.0.x"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 123456789,
        "free": 98765432,
        "threshold": 10485760,
        "path": "/path/to/api",
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Response** (when unhealthy):
```json
{
  "status": "DOWN",
  "components": {
    "mongo": {
      "status": "DOWN",
      "details": {
        "error": "MongoTimeoutException: Timed out after 30000 ms while waiting to connect"
      }
    }
  }
}
```

**Authentication**: None (public endpoint for monitoring)

**Usage**: Container orchestration health checks, monitoring dashboards

---

### Spring Boot Actuator Info

**Endpoint**: `GET /actuator/info`

**Purpose**: Application metadata (not a business API)

**Response**:
```json
{
  "app": {
    "name": "api",
    "version": "0.0.1-SNAPSHOT",
    "description": "Reactive API for Mithril Vault"
  },
  "java": {
    "version": "21"
  },
  "spring": {
    "profiles": ["dev"]
  }
}
```

**Authentication**: None (public endpoint)

**Usage**: Deployment verification, version tracking

---

## Future API Contracts

When business features are implemented, their API contracts will be defined in their respective `contracts/` directories:

- **Feature 002**: User authentication and authorization (future)
  - `POST /api/v1/auth/register` - Create new user account
  - `POST /api/v1/auth/login` - Authenticate and receive JWT token
  - `POST /api/v1/auth/refresh` - Refresh JWT token using refresh token
  - **Note**: No logout endpoint - API is stateless. Logout handled client-side by discarding JWT. Token blacklisting (if needed for security) will be addressed in that feature's specification.

- **Feature 003**: Account management (future)
  - `GET /api/v1/accounts`
  - `POST /api/v1/accounts`
  - `GET /api/v1/accounts/{id}`
  - `PUT /api/v1/accounts/{id}`
  - `DELETE /api/v1/accounts/{id}`

- **Feature 004**: Transaction management (future)
  - `GET /api/v1/transactions`
  - `POST /api/v1/transactions`
  - `GET /api/v1/transactions/{id}`
  - `PUT /api/v1/transactions/{id}`
  - `DELETE /api/v1/transactions/{id}`

Each feature will maintain OpenAPI 3.0+ specifications in its `contracts/` directory following the schema-first design principle (Constitution Principle VIII).

## CORS Configuration Contract

The CORS configuration establishes which origins can call the API:

**Allowed Methods**: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`  
**Allowed Headers**: `*` (all headers)  
**Allow Credentials**: `true` (for cookie-based auth)  
**Max Age**: `3600` seconds (1 hour preflight cache)  
**Allowed Origins**: Environment-specific (from `CORS_ALLOWED_ORIGINS` env var)

**Development Origins**:
- `http://localhost:3000`
- `http://127.0.0.1:3000`

**Production Origins**: TBD (will be configured via environment variable in deployment)

This establishes a contract between frontend and backend for cross-origin requests.
