# Mithril Vault

Personal-finance application (BRL, single-user, renda fixa). Polyglot monorepo: a reactive
Spring Boot API and a Next.js web client.

> **Source of truth for architecture:** `docs/architecture-contract.md`. This file and the
> per-layer `CLAUDE.md` files are the day-to-day working guide; when they and the contract
> disagree, the contract wins.
>
> **Product spec:** `docs/product-definition.md` and `docs/functional-specification.md`.
> Note: the product was scoped single-user, but this project is built **multi-user** (a
> deliberate learning goal — see the contract P2 and §8).

## Repository map

| Path | What |
|---|---|
| `api/` | Reactive Spring Boot backend (Java 21, WebFlux, MongoDB). See `api/CLAUDE.md`. |
| `web/` | Next.js 16 / React 19 frontend (TypeScript, Tailwind, shadcn/ui). See `web/CLAUDE.md`. |
| `docs/` | Product & functional definitions + the architecture contract. |
| `specs/` | Per-feature OpenAPI contracts & data models (schema-first; created per feature). |

## Foundational rule — money is `Long` centavos

All monetary values are integers in **centavos** (1 real = 100 centavos) at every layer: Java
`Long`, MongoDB `Int64` (never `Double`), JSON integer, frontend `number` formatted only at
render. This eliminates floating-point error. **Never use `BigDecimal`, `double`, or `float`
for money** anywhere in the stack.

Percentage arithmetic: **always multiply before dividing** to preserve precision.

```java
long ir = (grossYield * 225) / 1000;   // ✓ 22.5%
long ir = grossYield / 1000 * 225;     // ✗ truncates
```

Frontend formats centavos only at render time:

```typescript
const formatBRL = (centavos: number): string =>
  (centavos / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
// 150090 → "R$ 1.500,90"
```

## Global engineering standards

- **Self-documenting code — no comments.** If logic needs a comment, extract a well-named
  private method/function instead. Names describe content and purpose.
- **Conventional Commits** (`feat:`, `fix:`, `chore:`, `docs:`, …).
- **Zero-trust security.** Never commit or output secrets/API keys. Treat all user input as
  untrusted and validate it at the edge. Never store card PAN or CVV; hash passwords (BCrypt/Argon2).
- **Schema-first.** Define OpenAPI contracts and MongoDB schemas before implementation
  (`specs/[###-feature]/contracts/`, `data-model.md`).

## Run locally

```bash
docker compose up -d mongodb                 # MongoDB on :27017
cd api && ./gradlew bootRun                   # API → http://localhost:8080/mithril-vault
                                              # Swagger → /mithril-vault/swagger-ui.html
cd web && pnpm install && pnpm dev            # Web → http://localhost:3000
```

## Migration note

This project moved from GitHub Copilot to Claude Code. The former Copilot files said "Spring
Boot 3" and "use BigDecimal for money" — both **superseded**: the stack is **Spring Boot 4.0.6**
(`api/gradle/libs.versions.toml`) and money is **`Long` centavos** per the finalized product
definition.
