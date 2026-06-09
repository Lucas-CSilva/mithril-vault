# Data Model — Auth (001)

## Collections

### `users`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Primary key |
| `email` | String | Unique (case-insensitive via collation). Login identifier. |
| `passwordHash` | String | BCrypt or Argon2 hash. **Never returned in any response.** |
| `displayName` | String | Optional. |
| `status` | String | Enum: `ACTIVE`, `DISABLED` |
| `createdAt` | Date | UTC instant |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `email` | Unique | collation `{ locale: "en", strength: 2 }` | Enforce unique email, case-insensitive |

No `ownerId` on User itself — User is the root tenant.

---

### `refresh_tokens`

| Field | BSON Type | Notes |
|---|---|---|
| `_id` | String (UUID) | Token id (also the `jti` claim in the refresh JWT) |
| `userId` | String (UUID) | FK → users |
| `tokenHash` | String | SHA-256 of the raw token value. Only the hash is stored. |
| `expiresAt` | Date | UTC instant (~30 days from issuance) |
| `revokedAt` | Date | Nullable. Set on logout or token rotation. |
| `replacedBy` | String (UUID) | Nullable. Id of the new token issued on rotation (audit trail). |
| `createdAt` | Date | UTC instant |

**Indexes:**

| Fields | Type | Options | Purpose |
|---|---|---|---|
| `userId` | Non-unique | — | Look up tokens by user (e.g., invalidate all on password change) |
| `expiresAt` | TTL | `expireAfterSeconds: 0` | Auto-remove expired tokens from MongoDB |
| `tokenHash` | Unique | sparse | Fast lookup on incoming token; enforce no hash collision |

---

## Relationships

```
users (1) ──< refresh_tokens (many)
```

`refresh_tokens.userId` references `users._id`. No MongoDB `$ref` — plain UUID string FK.

---

## Security Notes

- `passwordHash` must never appear in any API response or log line.
- The raw refresh token is never stored — only `SHA-256(rawToken)`.
- On rotation (every use), the old token's `replacedBy` is set to the new token's id, then it is revoked. This gives an audit trail and detects token theft (using a revoked token family → invalidate all family tokens for that user).
- `expiresAt` TTL index automatically purges expired tokens; application code must also reject tokens where `revokedAt IS NOT NULL`.
