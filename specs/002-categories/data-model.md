# Data Model — Categories (002)

## Collections

### `categories`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `name` | String | Required |
| `parentId` | String (UUID) | Nullable. References another `categories._id`. Hierarchy max depth = 1 (two levels only). |
| `icon` | String | Icon identifier (e.g. emoji or icon library key) |
| `color` | String | Hex color code, e.g. `#88C0D0` |
| `isSystem` | Boolean | `true` = system category (global, indestructible). `false` = user-defined. |
| `ownerId` | String (UUID) | Nullable. `null` for system categories (shared, read-only). UUID for user-defined categories. |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | sparse | Tenant-scoped listing queries |
| `isSystem` | Non-unique | — | Seed idempotency check |
| `{ ownerId: 1, name: 1 }` | Unique | sparse, collation `{ locale: "pt", strength: 2 }` | Prevent duplicate category names per user (case-insensitive). Does not apply to system categories (ownerId = null). |

---

## System Categories (seed data)

Inserted at application startup by an idempotent `ApplicationRunner`. These rows have `ownerId = null` and `isSystem = true`. They must never be modified or deleted through the API.

| Name | Icon | Subcategories |
|---|---|---|
| Alimentação | 🍽️ | Supermercado, Delivery, Restaurante, Padaria |
| Moradia | 🏠 | Aluguel, Energia, Água, Internet, Condomínio |
| Transporte | 🚗 | Combustível, Aplicativo, Transporte Público, Manutenção |
| Saúde | 💊 | Farmácia, Consulta, Academia, Plano de Saúde |
| Educação | 📚 | Cursos, Material, Mensalidade |
| Lazer | 🎭 | Cinema, Viagem, Entretenimento, Hobby |
| Vestuário | 👗 | Roupas, Calçados, Acessórios |
| Serviços & Assinaturas | 📱 | Streaming, Software, Telefone |
| Investimentos | 📈 | Renda Fixa, Tesouro Direto, Ações |
| Transferências | 🔄 | — (top-level, no subcategories) |
| Renda | 💰 | Salário, Freelance, Transferência Recebida, Dividendos |
| Outros | 📦 | — (top-level; the fallback for deleted categories) |

---

## Relationships

```
categories (0..1) ──< categories (many)    [self-referential: parent → children]
categories (1) ──< transactions (many)
categories (1) ──< budgets (many)
```

`categories.parentId` references `categories._id`. Max depth = 1 enforced in the application layer — a child category cannot itself be a parent.

---

## Invariants

- `isSystem = true` → `ownerId = null`. The combination `isSystem = true` with a non-null `ownerId` is invalid.
- A user-defined top-level category has `parentId = null` and `ownerId = <userId>`.
- A user-defined subcategory has `parentId = <parentCategoryId>` and `ownerId = <userId>`. The parent must exist and belong to the same owner (or be a system category).
- Deleting a user-defined category: all `transactions.categoryId` pointing to it (and its children) must be bulk-reassigned to the system "Outros" category atomically (reactive transaction, P7) before the category document is deleted. Deletion is never blocked.
- System categories are never deleted. The API returns 403 if a delete attempt targets `isSystem = true`.
