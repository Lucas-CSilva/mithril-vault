# Mithril Vault — Frontend (`web/`)

Next.js 16 (App Router) / React 19 / TypeScript 5 / Tailwind 4 / shadcn/ui. Architecture is
governed by `docs/architecture-contract.md` (P9–P11); this is the working summary.

shadcn/ui workflow: @./.claude/docs/shadcn-workflow.md

## Architecture — Feature-Sliced + Hexagonal (MANDATORY)

```
src/
├── app/            # Next.js App Router — ROUTING ONLY. Pages delegate to features. No logic.
│   ├── (auth)/     # public route group (login, register)
│   ├── (app)/      # authenticated route group — gated by middleware.ts
│   └── layout.tsx  # root providers: QueryClient, NotificationProvider
├── features/       # Business verticals: accounts, cards, dashboard, investments,
│   └── <name>/         planning, subscriptions. Self-contained:
│       ├── components/  # feature-specific components (Server + Client)
│       ├── hooks/       # React Query hooks (useAccounts, useCreateTransaction)
│       ├── api.ts       # calls core/services singleton — NO raw fetch
│       ├── keys.ts      # query-key factory for this feature
│       ├── types.ts     # hand-written, mirrors backend Records exactly
│       └── schema.ts    # zod form schemas (when the feature has forms)
├── core/           # Typed contracts + singleton implementations
│   ├── ports/      # interfaces (ApiClient, AuthGateway, …)
│   └── services/   # singleton implementations (httpApiClient, …) — imported directly
└── shared/         # Pure UI + utils, NO domain knowledge
    ├── components/ui/   # shadcn primitives (alias @/shared/components/ui)
    ├── hooks/
    ├── types/      # Centavos brand + cross-cutting types
    └── utils/      # formatters (formatBRL), cn, reais↔centavos, helpers
```

**Import boundaries:**
- `app/` → `features/`, `core/`, `shared/`, Next APIs.
- `features/` → `core/` (ports + services), `shared/`, React.
- `core/` → `shared/` + external libs + React.
- `shared/` → React + Tailwind only (no `core/`, no `features/`).
- **Forbidden:** `core/` or `shared/` importing `features/`; one `features/` importing another
  (extract to `shared/` instead). Enforced via `eslint-plugin-boundaries` — this is what makes
  the boundaries real. No `index.ts` barrel files needed; the lint rule owns encapsulation.

## Core — ports and services

Features call `api.ts`, which imports singleton services from `core/services`. No context providers, no DI container.

- **Ports** (`core/ports`): TypeScript interfaces — `ApiClient`, `AuthGateway`, etc. No `I` prefix.
- **Services** (`core/services`): singleton implementations exported as module-level instances —
  `httpApiClient` wraps `fetch` with base URL, JSON, `credentials: 'include'`, and centralized
  error/401 handling (see Auth below). Import directly: `import { httpApiClient } from '@/core/services/httpApiClient'`.
- A feature **MUST NOT** call `fetch` directly — that is a lint error. All HTTP goes through `api.ts` → `core/services`.

## React / Next.js

- **Server Components first.** Add `'use client'` only when you need hooks (`useState`/`useEffect`)
  or event handlers. Initial-render data is fetched in RSC; interactive server-state uses React
  Query in client components.
- **Four states per async view:** loading (`Suspense` + skeletons), error (error boundary +
  retry), empty, loaded. Empty/error are designed, not afterthoughts.
- Global state: React Context (auth, theme, notifications). UI-only state: `useState`. Avoid Redux.

## Data layer — React Query (the only client server-state lib)

- React Query is the single client-side server-state library. No SWR, no ad-hoc `useEffect`
  fetching. Server-state lives in the query cache; React state is for UI only.
- **Query keys** come from a per-feature factory in `keys.ts`, hierarchical and typed —
  `accountKeys.all = ['accounts'] as const`, `accountKeys.detail(id) = ['accounts', id] as const`.
  Never hand-build key arrays inline in a component.
- **Hook naming:** reads `use<Plural>()` / `use<Entity>(id)`; mutations `use<Verb><Entity>()`
  (`useCreateTransaction`). Feature hooks call `api.ts`, which calls the `core/services` singleton.
- **Invalidation discipline:** a mutation invalidates **every** query it affects, including
  cross-feature. A transaction write invalidates transactions **and** account balances, the
  relevant budget(s), and the dashboard. Derived reads are **refetched, never hand-patched**
  (contract P4: derived values have one owner — the read side).

## Auth & Server/Client boundary

- **Tokens live in `httpOnly`, `Secure`, `SameSite` cookies** — never `localStorage`/JS storage.
  `ApiClient` sends `credentials: 'include'`; RSC fetches forward the incoming cookies.
- **401 handling is centralized in `ApiClient`:** on 401 it tries one `POST /auth/refresh` and
  retries once; a failed refresh clears session → redirect to login. Features never do refresh.
- **Route gating** in `middleware.ts` for `(app)` (redirect unauthenticated → login). This is
  defense-in-depth UX, **not** the security boundary — the API enforces authz (contract P2).

## TypeScript

- **Strict, no `any`.** Define types for all props and API responses.
- **Hand-write** each feature's `types.ts` to mirror the backend response Records exactly (same
  field names + shape). We do not codegen — the manual mirror is the FE↔API agreement checkpoint.
  **No `I` prefix** — `Account`, not `IAccount`. Components PascalCase; hooks `use*`; API fns
  camelCase.

## Money — branded `Centavos`

Money is a **branded `Centavos`** type (defined once in `shared/types`), not a bare `number`, so
a raw reais value can't flow where centavos are expected without an explicit conversion:

```typescript
export type Centavos = number & { readonly __brand: 'Centavos' };
export const centavos = (n: number): Centavos => n as Centavos;
export const formatBRL = (value: Centavos): string =>
  (value / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
```

Type every monetary field `Centavos`. `formatBRL` is the **only** place `/ 100` happens, at
render only. Never use floats for money math; never store/compute display strings.

## Styling — Nord theme, LIGHT mode (Snow Storm)

Tailwind utilities only. **No CSS-in-JS**, no global CSS outside `globals.css`, no inline styles
except dynamic chart calculations. Built on shadcn/ui (config: `components.json`, "new-york").

| Group | Use | Hex |
|---|---|---|
| Snow Storm | Backgrounds (light) / text on dark | `#ECEFF4` `#E5E9F0` `#D8DEE9` |
| Polar Night | Text on light / dark surfaces | `#2E3440` `#3B4252` `#434C5E` `#4C566A` |
| Frost | Primary actions / positive | `#8FBCBB` `#88C0D0` `#81A1C1` `#5E81AC` |
| Aurora | Semantics | `#BF616A` error · `#D08770` warning · `#EBCB8B` yellow · `#A3BE8C` success |

Fonts: Geist Sans (primary), Geist Mono (numeric/code).

## Forms & notifications

- `react-hook-form` + `zod` (resolver via `@hookform/resolvers`). Co-locate the schema in the
  feature's `schema.ts`; mirror backend Jakarta constraints. Convert reais→`Centavos` at the form
  edge before sending (single conversion, never round-trip through float).
- **Notifications:** one app-level notifications context drives toasts. Budget threshold alerts
  (80% / 100%) and mutation success/failure surface through it — not scattered local toast calls.

## Testing

React Testing Library — test user behavior, not implementation details. Coverage percentages are
advisory signals, not merge gates (contract P11); we gate on meaningful assertions being present.

## Commands (pnpm)

```bash
pnpm dev            # dev server → http://localhost:3000
pnpm build          # production build
pnpm lint           # ESLint     (pnpm lint:fix to autofix)
pnpm format         # Prettier   (pnpm format:check to verify) — also runs on edit via hook
```
