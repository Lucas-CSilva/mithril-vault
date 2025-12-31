# Data Model: Development Environment Setup

**Feature**: 001-environment-setup  
**Phase**: 1 (Design & Contracts)  
**Date**: December 31, 2025

## Purpose

This document describes any data structures or entities related to the development environment setup feature.

## Data Modeling Assessment

**Status**: N/A - No Data Modeling Required

**Rationale**: This is a pure infrastructure/configuration feature that does not introduce any business entities, database collections, or persistent data structures. The feature focuses on:

1. Configuring MongoDB connection (infrastructure concern)
2. Setting up application configuration files
3. Configuring frontend build tools and styling
4. Establishing directory structure

No MongoDB collections will be created as part of this feature. No domain entities are introduced. Future features that implement business logic will define their own data models in their respective `data-model.md` files.

## Configuration Data Structures

While no persistent data is modeled, the following configuration structures are relevant:

### Spring Boot Configuration Structure

```yaml
# Logical structure of application.yaml
spring:
  application:
    name: string
  data:
    mongodb:
      uri: string (environment variable reference)
      connection-pool:
        min-size: integer
        max-size: integer
        max-wait-time: duration
        max-connection-life-time: duration
        max-connection-idle-time: duration
      
management:
  endpoints:
    web:
      exposure:
        include: array<string>
      base-path: string
  endpoint:
    health:
      show-details: enum(always|when-authorized|never)

app:
  cors:
    allowed-origins: array<string>
```

### Next.js Configuration Structure

```typescript
// next.config.ts type structure
interface NextConfig {
  reactStrictMode: boolean;
  experimental?: {
    optimizePackageImports?: string[];
  };
  env?: Record<string, string>;
}
```

### Theme Configuration Structure

```typescript
// src/config/theme.ts structure
interface NordTheme {
  colors: {
    polarNight: {
      nord0: string;  // Darkest background
      nord1: string;  // Dark surface
      nord2: string;  // Medium border
      nord3: string;  // Light disabled
    };
    snowStorm: {
      nord4: string;  // Light text
      nord5: string;  // Primary text
      nord6: string;  // Brightest background
    };
    frost: {
      nord7: string;  // Teal secondary
      nord8: string;  // Cyan primary
      nord9: string;  // Blue link
      nord10: string; // Dark blue visited
    };
    aurora: {
      nord11: string; // Red error
      nord12: string; // Orange warning
      nord13: string; // Yellow caution
      nord14: string; // Green success
      nord15: string; // Purple special
    };
  };
  semantic: {
    primary: string;    // Maps to nord8
    secondary: string;  // Maps to nord7
    success: string;    // Maps to nord14
    warning: string;    // Maps to nord12
    error: string;      // Maps to nord11
    info: string;       // Maps to nord9
  };
}
```

## Environment Variables Data Contract

The following environment variables constitute a data contract between deployment environment and application:

### Backend Environment Variables

| Variable | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `MONGODB_URI` | string (URI) | Yes | - | MongoDB connection string with auth |
| `CORS_ALLOWED_ORIGINS` | string (CSV) | Yes | - | Comma-separated list of allowed origins |
| `SPRING_PROFILES_ACTIVE` | string | No | `dev` | Active Spring profile (dev/test/prod) |
| `SERVER_PORT` | integer | No | `8080` | API server port |
| `LOG_LEVEL` | enum | No | `INFO` | Logging level (TRACE/DEBUG/INFO/WARN/ERROR) |

### Frontend Environment Variables

| Variable | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | string (URL) | Yes | - | Backend API base URL |
| `NODE_ENV` | enum | No | `development` | Node environment (development/production) |
| `PORT` | integer | No | `3000` | Next.js dev server port |

### Docker Compose Environment Variables

| Variable | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `MONGODB_ROOT_USER` | string | No | `root` | MongoDB root username |
| `MONGODB_ROOT_PASSWORD` | string | No | `root` | MongoDB root password |
| `MONGODB_DATABASE` | string | No | `mithril_vault` | Initial database name |
| `MONGODB_PORT` | integer | No | `27017` | MongoDB exposed port |

## Validation Rules

### MongoDB URI Validation
- Must start with `mongodb://` or `mongodb+srv://`
- Must include authentication credentials in format `username:password@`
- Must specify host and port (or cluster for srv)
- Must specify database name
- Example: `mongodb://user:pass@localhost:27017/mithril_vault?authSource=admin`

### CORS Origins Validation
- Must be valid URLs with protocol (http:// or https://)
- No trailing slashes
- Comma-separated for multiple origins
- Example: `http://localhost:3000,https://app.mithrilvault.com`

### API URL Validation
- Must be valid URL with protocol
- No trailing slash
- Must be accessible from browser (for NEXT_PUBLIC_ vars)
- Example: `http://localhost:8080`

## Future Data Modeling

When business features are implemented, this section will reference their data models:

- **Feature 002**: Accounts & Transactions data model (future)
- **Feature 003**: Budget & Planning data model (future)
- **Feature 004**: Investment tracking data model (future)

Each feature will maintain its own `data-model.md` with MongoDB collection schemas, indexes, and relationships.
