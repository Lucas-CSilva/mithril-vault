# Data Model — Import (006)

## Collections

### `import_mappings`

Persists the user's CSV column mapping so it can be reused on subsequent imports from the same source.

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `ownerId` | String (UUID) | FK → users. Immutable. |
| `sourceName` | String | User-given label for this source (e.g. "Bradesco CSV") |
| `dateColumn` | String | Column name or index for the date field |
| `descriptionColumn` | String | Column name or index for the description field |
| `amountColumn` | String | Column name or index for the amount field |
| `typeColumn` | String | Nullable. Column name/index that distinguishes DEBIT from CREDIT (if separate from amount sign) |
| `dateFormat` | String | e.g. `dd/MM/yyyy`, `yyyy-MM-dd` |
| `amountLocale` | String | e.g. `pt-BR` (determines decimal/thousand separator) |
| `hasHeader` | Boolean | Whether the CSV has a header row |
| `delimiter` | String | `,` or `;` |
| `createdAt` | Date | UTC instant |
| `updatedAt` | Date | UTC instant |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `ownerId` | Non-unique | — | Required by P2 |
| `{ ownerId: 1, sourceName: 1 }` | Unique | collation `{ locale: "pt", strength: 2 }` | One mapping per source name per user |

---

## Import Flow (no collection — creates transactions)

The import process does not have its own persistent collection beyond `import_mappings`. The result of a confirmed import is a set of `transactions` documents.

### Deduplication Keys (checked against `transactions`)

| Format | Key field | Value |
|---|---|---|
| OFX | `transactions.fitid` | Bank-provided FITID from OFX `<FITID>` tag |
| CSV | `transactions.importHash` | `SHA-256(UPPERCASE(date) + UPPERCASE(TRIM(description)) + amount)` |

Before creating any transaction, the handler queries:
- For OFX: `{ ownerId: X, fitid: { $in: [list of FITIDs] } }`
- For CSV: `{ ownerId: X, importHash: { $in: [list of hashes] } }`

Matching rows are returned to the frontend as `isDuplicate: true` and pre-unchecked. The user may re-check them to force re-import.

---

## OFX Parsing Notes

OFX files from Brazilian banks are typically SGML (OFX 1.x), not XML. The parser must handle:
- `<OFX>...<BANKTRANLIST>...<STMTTRN>` structure
- Fields: `<FITID>`, `<DTPOSTED>`, `<TRNAMT>`, `<MEMO>` or `<NAME>`
- Amount sign: negative = DEBIT, positive = CREDIT
- Date format: `YYYYMMDD` or `YYYYMMDDHHmmss[TZ]`

---

## Category Auto-Suggestion

Applied during preview (before confirmation). The keyword map from `docs/product-definition.md` is evaluated against each transaction's normalized description (uppercase). The suggested `categoryId` is returned as a hint — the user can override it before confirming.
