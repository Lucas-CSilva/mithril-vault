# Data Model — Investments (010)

## Collections

### `investments`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. |
| `name` | String | e.g. "CDB Nubank 110% CDI" |
| `type` | String | Enum: `CDB`, `TESOURO_SELIC`, `TESOURO_IPCA`, `TESOURO_PREFIXADO`, `LCI`, `LCA`, `CRI`, `CRA`, `DEBENTURE` |
| `institution` | String | |
| `investedAmount` | Int64 | Centavos. Original principal. Immutable after creation. |
| `currentGrossValue` | Int64 | Centavos. User-updated. Defaults to `investedAmount` on creation. |
| `rateType` | String | Enum: `CDI_PERCENTAGE`, `IPCA_PLUS`, `PREFIXADO`, `SELIC_PERCENTAGE` |
| `rateValue` | Int64 | Rate × 100 to avoid decimals. e.g. `11000` = 110.00% CDI; `650` = 6.50% a.a. |
| `startDate` | Date | LocalDate |
| `maturityDate` | Date | Nullable. LocalDate. |
| `liquidityType` | String | Enum: `DAILY`, `AT_MATURITY`, `SPECIFIC_DATE` |
| `liquidityDate` | Date | Nullable. Only when `liquidityType = SPECIFIC_DATE`. |
| `isIRExempt` | Boolean | `true` for LCI, LCA (also CRI, CRA, Debênture incentivada) |
| `status` | String | Enum: `ACTIVE`, `MATURED`, `REDEEMED` |
| `redeemedAt` | Date | Nullable. LocalDate. |
| `redeemedAmount` | Int64 | Nullable. Centavos. Actual net amount received on redemption. |
| `valueHistory` | Array | Embedded. Append-only list of `ValueEntry` (see below). |
| `createdAt` | Date | UTC instant |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |
| `{ ownerId: 1, status: 1 }` | Non-unique | — | Portfolio summary (filter ACTIVE) |
| `{ ownerId: 1, maturityDate: 1 }` | Non-unique | sparse | Maturity alert job |

---

### Embedded: `ValueEntry`

Each element in `investments.valueHistory`:

| Field | BSON Type | Notes |
|---|---|---|
| `grossValue` | Int64 | Centavos. The gross value at the time of this update. |
| `recordedAt` | Date | UTC instant. |

`valueHistory` is append-only. `currentGrossValue` is always the `grossValue` of the most recent entry (or `investedAmount` if no updates yet).

---

## IR Calculation (non-exempt investments)

```
daysSinceStart  = DAYS_BETWEEN(startDate, today)
irRate          = irRateFromTable(daysSinceStart)   // Int64, stored as rate × 100
grossYield      = currentGrossValue - investedAmount
estimatedIR     = (grossYield × irRate) / 10000     // multiply before divide (P1)
netValue        = currentGrossValue - estimatedIR
```

**IR rate table:**

| Days held | IR rate | Stored `irRate` |
|---|---|---|
| ≤ 180 | 22.5% | 2250 |
| 181 – 360 | 20.0% | 2000 |
| 361 – 720 | 17.5% | 1750 |
| > 720 | 15.0% | 1500 |

`isIRExempt = true` → `irRate = 0` → `estimatedIR = 0` → `netValue = currentGrossValue`.

Displayed as an **estimate** — actual tax is determined by the broker.

---

## Account Integration (optional, P2)

When creating an investment:
- Checkbox "Registrar débito na conta" → create a DEBIT transaction on the source account for `investedAmount`

When redeeming:
- Checkbox "Registrar crédito na conta" → create a CREDIT transaction on the destination account for `redeemedAmount`

These transactions are created atomically with the investment operation (reactive transaction, P7).
