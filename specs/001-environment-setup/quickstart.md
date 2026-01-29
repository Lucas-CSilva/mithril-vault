# Developer Quickstart Guide: Mithril Vault

**Feature**: 001-environment-setup  
**Last Updated**: December 31, 2025  
**Target Audience**: New developers joining the project

## Prerequisites

Before you begin, ensure you have the following installed:

- **Git**: Version control
- **Docker & Docker Compose**: For running MongoDB locally
- **Java 21**: Backend runtime (JDK, not just JRE)
- **Node.js 20.x**: Frontend runtime (LTS version)
- **pnpm**: Fast, disk space efficient package manager
- **Your favorite IDE**: VS Code, IntelliJ IDEA, or similar

### Verify Prerequisites

```bash
# Check versions
git --version                 # Should be 2.x or higher
docker --version              # Should be 24.x or higher
docker-compose --version      # Should be 2.x or higher
java -version                 # Should show "21.x.x"
node --version                # Should be v20.x.x
pnpm --version                # Should be 9.x or higher

# Install pnpm if not already installed
npm install -g pnpm
```

## Quick Start (15 Minutes)

### 1. Clone the Repository

```bash
git clone https://github.com/Lucas-CSilva/mithril-vault.git
cd mithril-vault
```

### 2. Start MongoDB

```bash
# Start MongoDB in the background
docker-compose up -d

# Verify MongoDB is running
docker-compose ps
# Should show "mithril_mongodb" with status "Up"

# Check MongoDB logs (optional)
docker-compose logs -f mongodb
```

### 3. Configure Environment Variables

```bash
# Copy example environment files
cp .env.example .env
cp api/.env.example api/.env
cp web/.env.example web/.env.local

# Edit files if needed (defaults work for local development)
# Optional: Use your preferred editor to modify values
```

**Default values work out of the box** for local development. No changes needed unless you:
- Changed MongoDB ports in docker-compose
- Want to customize application ports
- Need to connect to a remote database

### 4. Start the Backend API

```bash
# Navigate to API directory
cd api

# Run the application (Gradle wrapper downloads Gradle automatically)
./gradlew bootRun

# Wait for startup message:
# "Started ApiApplication in X.XXX seconds"

# Verify health (in a new terminal):
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP","components":{...}}
```

**Backend will be running on**: `http://localhost:8080`

### 5. Start the Frontend

```bash
# Navigate to web directory (from project root)
cd web

# Install dependencies (first time only, takes 1-2 minutes)
pnpm install

# Start the development server
pnpm dev

# Wait for the message:
# "✓ Ready in X.Xs"
# "○ Local: http://localhost:3000"
```

**Frontend will be running on**: `http://localhost:3000`

### 6. Verify Everything Works

1. **Open your browser**: Navigate to `http://localhost:3000`
2. **You should see**: The Mithril Vault landing page
3. **Open DevTools Network tab**: Verify no CORS errors
4. **Check backend health**: Visit `http://localhost:8080/actuator/health` (should show UP status)

**Congratulations!** 🎉 Your development environment is ready.

---

## Project Structure Overview

```
mithril-vault/
├── api/                    # Backend (Spring Boot + Java 21)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/.../api/
│   │   │   │   ├── domain/          # Business logic (pure, no dependencies)
│   │   │   │   ├── application/     # REST controllers (HTTP layer)
│   │   │   │   └── infrastructure/  # DB, configs (Spring, MongoDB)
│   │   │   └── resources/
│   │   │       ├── application.yaml      # Main config
│   │   │       ├── application-dev.yaml  # Dev overrides
│   │   │       └── application-prod.yaml # Prod overrides
│   │   └── test/           # Tests (unit + integration)
│   ├── build.gradle        # Dependencies and build config
│   └── gradlew            # Gradle wrapper (no installation needed)
│
├── web/                    # Frontend (Next.js + React + TypeScript)
│   ├── src/
│   │   ├── app/           # Next.js routing (App Router)
│   │   ├── features/      # Business features (FSD architecture)
│   │   ├── core/          # Dependency injection, services
│   │   ├── shared/        # Reusable UI components
│   │   └── config/        # Theme, API client configs
│   ├── package.json       # Dependencies
│   └── next.config.ts     # Next.js configuration
│
├── specs/                  # Feature specifications and plans
│   └── 001-environment-setup/
│       ├── spec.md        # Requirements
│       ├── plan.md        # Technical plan
│       ├── research.md    # Technology decisions
│       └── quickstart.md  # This file
│
├── docker-compose.yml      # Local infrastructure (MongoDB)
└── .env.example           # Environment variable template
```

---

## Common Development Tasks

### Running Tests

```bash
# Backend tests
cd api
./gradlew test

# Frontend tests (when implemented)
cd web
pnpm test
```

### Code Formatting

```bash
# Backend (Spotless - Google Java Format)
cd api
./gradlew spotlessApply   # Auto-format all Java files

# Frontend (Prettier via Next.js)
cd web
pnpm lint              # Check for issues
pnpm lint --fix        # Auto-fix issues
```

### Building for Production

```bash
# Backend JAR
cd api
./gradlew build
# Output: api/build/libs/api-0.0.1-SNAPSHOT.jar

# Frontend static export
cd web
pnpm build
# Output: web/.next/ directory
```

### Database Management

```bash
# View MongoDB logs
docker-compose logs -f mongodb

# Access MongoDB shell
docker exec -it mithril_mongodb mongosh -u root -p root

# Stop MongoDB
docker-compose down

# Stop and remove data (reset database)
docker-compose down -v
```

### Switching Spring Profiles

```bash
# Run with specific profile
cd api
SPRING_PROFILES_ACTIVE=test ./gradlew bootRun

# Or set in .env file:
echo "SPRING_PROFILES_ACTIVE=test" >> api/.env
```

---

## Development Workflow

### Feature Development Process

1. **Create feature branch**: `git checkout -b 00X-feature-name`
2. **Write specification**: Create `specs/00X-feature-name/spec.md`
3. **Create implementation plan**: Generate `specs/00X-feature-name/plan.md`
4. **Implement backend** (if needed):
   - Define domain models in `api/src/main/java/.../domain/model/`
   - Create commands and handlers
   - Implement repositories in `infrastructure/`
   - Add controllers in `application/`
   - Write tests for all layers
5. **Implement frontend** (if needed):
   - Create feature directory in `web/src/features/[feature-name]/`
   - Build components, hooks, and types
   - Add routing in `web/src/app/[feature-name]/`
   - Write component tests
6. **Test end-to-end**: Verify feature works in browser
7. **Format and lint**: Run `spotlessApply` and `pnpm lint --fix`
8. **Commit and push**: Follow conventional commits (`feat:`, `fix:`, `chore:`)
9. **Create pull request**: Request review

### Architecture Guidelines

**Backend (Hexagonal Architecture)**:
- ✅ Domain layer is pure Java (no Spring, no MongoDB)
- ✅ Use Commands and CommandHandlers for write operations
- ✅ Use Records or Lombok `@Value` for immutability
- ✅ MapStruct for DTO ↔ Entity mappings
- ❌ Never let domain layer depend on application/infrastructure

**Frontend (Feature-Sliced Design)**:
- ✅ Features are independent (no cross-feature imports)
- ✅ Use Tailwind utilities (no custom CSS)
- ✅ Nord theme colors only
- ✅ Shadcn/ui for UI components
- ❌ Never import from other features

See `.specify/memory/constitution.md` for complete architecture rules.

---

## Troubleshooting

### Problem: MongoDB connection fails

**Symptoms**: Backend logs show `MongoTimeoutException` or `Connection refused`

**Solutions**:
1. Verify MongoDB is running: `docker-compose ps`
2. Check MongoDB logs: `docker-compose logs mongodb`
3. Restart MongoDB: `docker-compose restart mongodb`
4. Verify connection string in `api/.env` matches docker-compose ports
5. Check firewall/antivirus blocking port 27017

---

### Problem: Frontend can't connect to backend

**Symptoms**: CORS errors in browser console, or "Network Error"

**Solutions**:
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check `NEXT_PUBLIC_API_URL` in `web/.env.local` is correct
3. Verify CORS origins in `api/src/main/resources/application.yaml` include `http://localhost:3000`
4. Restart both frontend and backend
5. Clear browser cache and hard reload (Ctrl+Shift+R)

---

### Problem: Port already in use

**Symptoms**: `Address already in use` error when starting services

**Solutions**:
```bash
# Find process using port (Linux/Mac)
lsof -i :8080   # Backend
lsof -i :3000   # Frontend
lsof -i :27017  # MongoDB

# Kill process
kill -9 <PID>

# Or change port in configuration
# Backend: SERVER_PORT=8081 in api/.env
# Frontend: PORT=3001 in web/.env.local
# MongoDB: MONGODB_PORT=27018 in .env (and update connection URI)
```

---

### Problem: Gradle build fails

**Symptoms**: Compilation errors, dependency resolution failures

**Solutions**:
1. Verify Java 21 is installed: `java -version`
2. Clean build: `./gradlew clean build`
3. Refresh dependencies: `./gradlew build --refresh-dependencies`
4. Check internet connection (Gradle downloads dependencies)
5. Delete `.gradle/` directory and rebuild

---

### Problem: npm install fails

**Symptoms**: Dependency resolution errors, peer dependency conflicts

**Solutions**:
1. Verify Node.js 20.x: `node --version`
2. Clear pnpm store: `pnpm store prune`
3. Delete `node_modules` and `pnpm-lock.yaml`: `rm -rf node_modules pnpm-lock.yaml`
4. Reinstall: `pnpm install`
5. If peer dependency issues persist: `pnpm install --no-strict-peer-dependencies`

---

## Environment Variables Reference

### Backend (`api/.env`)

```bash
# MongoDB Connection
MONGODB_URI=mongodb://root:root@localhost:27017/mithril_vault?authSource=admin

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000

# Application Settings
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
LOG_LEVEL=INFO
```

### Frontend (`web/.env.local`)

```bash
# API Connection
NEXT_PUBLIC_API_URL=http://localhost:8080

# Node Environment
NODE_ENV=development
PORT=3000
```

### Docker Compose (`.env`)

```bash
# MongoDB Settings
MONGODB_ROOT_USER=root
MONGODB_ROOT_PASSWORD=root
MONGODB_DATABASE=mithril_vault
MONGODB_PORT=27017
```

---

## Next Steps

Now that your environment is configured:

1. **Familiarize yourself with the codebase**: Explore `api/src/` and `web/src/`
2. **Read the constitution**: `.specify/memory/constitution.md` (architecture rules)
3. **Review existing features**: Check `specs/` directory
4. **Pick up a task**: Look for issues labeled `good-first-issue`
5. **Ask questions**: Reach out to the team on Slack/Discord

**Happy coding!** 🚀

---

## Additional Resources

- **Project Documentation**: `/docs/` directory
- **Constitution (Architecture Rules)**: `.specify/memory/constitution.md`
- **Feature Specifications**: `/specs/` directory
- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/docs/current/reference/html/
- **Next.js Documentation**: https://nextjs.org/docs
- **Tailwind CSS**: https://tailwindcss.com/docs
- **Nord Theme**: https://www.nordtheme.com/
- **Shadcn/ui**: https://ui.shadcn.com/

---

**Questions or Issues?**  
Open an issue on GitHub or contact the team lead.
