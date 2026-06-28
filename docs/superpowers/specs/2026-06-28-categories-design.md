# Categories — Implementation Design

**Date:** 2026-06-28
**Feature:** 1.2 — Categories
**Specs:** `specs/002-categories/data-model.md`, `specs/002-categories/contracts/category.openapi.yaml`
**Status:** Approved

---

## Context

Categories are a foundational cross-cutting concern: every transaction, budget, and import row
references one. This feature delivers the full backend CRUD and a reusable `CategoryPicker`
frontend component with inline management. There is no dedicated categories page.

The API is an independent domain API (not BFF). It returns a flat `CategoryResponse[]`; the
frontend reconstructs the two-level tree client-side. This keeps the contract stable regardless
of how individual UIs render the data.

---

## Architecture

### Backend — hexagonal, mirroring the auth pattern

```
domain/
  model/          Category.java                    (Record)
  port/           CategoryRepository.java          (write port)
                  CategoryReadRepository.java      (read port)
  command/
    category/     CreateCategoryCommand.java
                  UpdateCategoryCommand.java
                  DeleteCategoryCommand.java
  commandhandler/
    category/     CreateCategoryCommandHandler.java
                  UpdateCategoryCommandHandler.java
                  DeleteCategoryCommandHandler.java
  query/
    category/     ListCategoriesQuery.java
                  ListCategoriesQueryHandler.java

infrastructure/
  persistence/
    document/     CategoryDocument.java
    repository/   CategoryMongoRepository.java
  adapter/        CategoryRepositoryAdapter.java
                  CategoryReadRepositoryAdapter.java
  config/         CategorySeeder.java              (ApplicationRunner)

application/
  controller/     CategoryController.java
  response/       CategoryResponse.java
```

### Domain model

```java
public record Category(
    String id,
    String name,
    String parentId,      // null for top-level
    String icon,
    String color,
    boolean isSystem,
    String ownerId        // null for system categories
) {}
```

### Frontend — feature slice

```
features/categories/
  types.ts          Category, CreateCategoryRequest, UpdateCategoryRequest
  api.ts            calls httpApiClient — list, create, update, delete
  keys.ts           categoryKeys factory
  hooks/
    useCategories.ts
    useCreateCategory.ts
    useUpdateCategory.ts
    useDeleteCategory.ts
  components/
    CategoryPicker.tsx    public component — Popover + Command + tree + management
    CategoryTree.tsx      accordion tree; accepts flat list, groups by parentId
    CategoryNode.tsx      single row; hover controls for user-defined categories
    CreateCategoryForm.tsx inline form rendered inside the popover
```

---

## Data Flow

### List
`GET /categories` returns the union of system categories (`ownerId = null`) and the
caller's own user-defined categories. The response is a flat array sorted: system categories
first (alphabetical), then user categories (alphabetical). Frontend builds the tree with a
single `groupBy(parentId)` pass — trivial at max depth = 1.

### Create
`POST /categories` with `{ name, parentId?, icon?, color? }`.

Handler validation order:
1. `parentId` present → verify it exists and is accessible to caller (owned or system).
2. Resolved parent's `parentId` must be `null` — enforces max depth = 1.
3. Insert document. Unique index on `{ ownerId, name }` (case-insensitive, sparse) catches
   duplicates → Mongo `DuplicateKeyException` → 409.

### Update
`PATCH /categories/{id}` with any subset of `{ name, icon, color }`.

`parentId` is immutable after creation — reparenting is not supported.
Handler blocks `isSystem = true` → 403.
Duplicate name on rename → 409 (same index).

### Delete
`DELETE /categories/{id}` — atomically, inside one reactive MongoDB session:

1. Collect IDs: `[id] + children where parentId = id`.
2. `updateMany` transactions where `categoryId in collectedIds` → set `categoryId` to the
   system "Outros" `_id`.
3. `deleteMany` children.
4. `deleteOne` the category.

If any step fails, the session rolls back. Returns 204 on success.
Handler blocks `isSystem = true` → 403.

### Seed
`CategorySeeder` (`ApplicationRunner`) runs at startup. For each of the 12 system categories:

1. `findByIsSystemTrueAndName(name)` — if present, skip.
2. Insert parent document (get `_id`).
3. Insert each subcategory with `parentId = parent._id`.

Idempotent: running twice is safe. Order is parent-first within each group.

System categories (from `specs/002-categories/data-model.md`): Alimentação, Moradia,
Transporte, Saúde, Educação, Lazer, Vestuário, Serviços & Assinaturas, Investimentos,
Transferências, Renda, Outros.

---

## CategoryPicker — UI design

A shadcn `Popover` wrapping a `Command` component. Layout inside the popover:

```
┌─────────────────────────────┐
│ 🔍 Search categories...     │
├─────────────────────────────┤
│ ▾ Alimentação        🍽️     │  ← system (no edit/delete icons)
│     Supermercado            │
│     Delivery                │
│ ▸ Moradia            🏠     │
│ ▾ Pets               🐾    │  ← user-defined (hover shows ✏️ 🗑️)
│     Ração                   │
├─────────────────────────────┤
│ + Nova categoria            │  ← opens CreateCategoryForm inline
└─────────────────────────────┘
```

- System categories: `isSystem = true` — no edit/delete icons rendered.
- User categories: pencil (edit) and trash (delete) icons on row hover.
- Clicking a leaf or a parentless top-level category selects it and closes the popover.
- Clicking a parent expands/collapses its children (accordion); selecting the parent itself
  is also valid.
- Search filters across all names (parents and children) simultaneously — flat data makes
  this a simple string filter before grouping.
- `CreateCategoryForm` renders inline at the bottom of the popover. On success, mutation
  invalidates `categoryKeys.all`; the new category appears in the tree immediately.
- Delete triggers a confirmation (`AlertDialog`) before calling the mutation.

---

## Error Handling

| Scenario | Backend | Frontend |
|---|---|---|
| Duplicate category name | Unique index → 409 | Toast: "Já existe uma categoria com esse nome" |
| Parent not found / not owned | 404 | Toast: "Categoria pai não encontrada" |
| Sub-subcategory attempt | 422 from handler | Inline form validation message |
| Delete / update system category | 403 | Edit/delete icons not rendered for `isSystem = true` |
| Category not found / not owned | 404 | Toast: generic not-found error |
| Transaction reassignment fails | Reactive tx rolls back → 500 | Toast: generic error; category remains intact |

---

## Testing

| Test | Type | What it covers |
|---|---|---|
| `CreateCategoryCommandHandlerTest` | Unit | Name uniqueness path, depth guard, system block |
| `UpdateCategoryCommandHandlerTest` | Unit | System block, immutable parentId |
| `DeleteCategoryCommandHandlerTest` | Unit | Reassignment count, system block, children cleanup |
| `CategoryControllerIT` | Integration (Testcontainers) | Full CRUD over HTTP, 403/404 semantics, tenancy isolation (user A cannot read/modify user B's categories) |
| `CategorySeederIT` | Integration | Running seeder twice yields exactly 12 system categories |

---

## Constraints & Invariants

- `isSystem = true` → `ownerId = null` always. The inverse (`isSystem = false`, `ownerId = null`) is invalid.
- Max depth = 1: a child category cannot itself be a parent. Enforced in `CreateCategoryCommandHandler`.
- `ownerId` is never returned in `CategoryResponse` (least disclosure, contract P6).
- `ownerId` is always extracted from the JWT, never from the request body (contract P2).
- Not-owned resource → 404, not 403 (contract P2 tenant isolation).
- The system "Outros" category is the hard-coded fallback for all category deletions; its `_id`
  is looked up at startup by `CategorySeeder` and held in a `@Value`-injected config or a
  thin `SystemCategoryIds` component.
- Monetary values: categories carry no money fields, so the `Long` centavos rule does not apply
  here, but the constraint is noted for completeness.
