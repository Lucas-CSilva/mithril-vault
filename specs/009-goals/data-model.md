# Data Model — Goals / Cofres (009)

## Collections

### `goals`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. |
| `name` | String | e.g. "Reserva de emergência", "Viagem para Europa" |
| `targetAmount` | Int64 | Centavos |
| `currentAmount` | Int64 | Centavos. Manually updated by user. |
| `deadline` | Date | Nullable. LocalDate (stored at midnight UTC). |
| `linkedAccountId` | String (UUID) | Nullable. FK → accounts. Informational only — goals do not move money. |
| `icon` | String | Icon identifier |
| `color` | String | Hex |
| `status` | String | Enum: `IN_PROGRESS`, `COMPLETED`, `ABANDONED` |
| `progressHistory` | Array | Embedded array of `ProgressEntry` (see below). Append-only. |
| `createdAt` | Date | UTC instant |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |
| `{ ownerId: 1, status: 1 }` | Non-unique | — | Filter by status |

---

### Embedded: `ProgressEntry`

Each element in `goals.progressHistory`:

| Field | BSON Type | Notes |
|---|---|---|
| `amount` | Int64 | Centavos. The new `currentAmount` at the time of this update. |
| `recordedAt` | Date | UTC instant when the update was recorded. |

`progressHistory` is append-only. Entries are never modified or deleted. It powers the projection calculation (avg monthly contribution over last 3 entries).

---

## Derived Values (computed, never stored)

### `requiredMonthly` (with deadline)

```
monthsRemaining = MONTHS_BETWEEN(today, deadline)   // rounded up
requiredMonthly = (targetAmount - currentAmount) / monthsRemaining
```

Returns 0 or a special flag if `monthsRemaining <= 0`.

### `projectedMonths` (without deadline)

```
avgMonthlyContribution = AVG(delta between last 3 progressHistory entries, normalized to monthly rate)
projectedMonths = (targetAmount - currentAmount) / avgMonthlyContribution
```

Returns null if fewer than 2 progress entries exist (not enough data).

---

## Status Transitions

```
IN_PROGRESS ──► COMPLETED   (when currentAmount >= targetAmount after a progress update)
IN_PROGRESS ──► ABANDONED   (manual user action)
COMPLETED   ──► IN_PROGRESS (user unarchives / adds more to target)
ABANDONED   ──► IN_PROGRESS (user reactivates)
```

Goals are never deleted (archive only).
