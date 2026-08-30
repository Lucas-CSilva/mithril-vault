# Task 009 — Controller wiring + indexes

## Scope

**In scope:** wire every handler built in tasks 002–008 into `TransactionController`
(`PATCH /transactions/{id}`, `GET /transactions`, `GET /transactions/{id}` — `POST /transactions`
and `GET /transactions/suggest-category` already exist/are added by 008), and add every index from
the updated `data-model.md` to `MongoIndexConfig` — including the new
`recurring_transaction_series` indexes.

**Explicitly not in scope:** `DELETE /transactions/{id}` — do not add this route. Its absence is
itself part of this task's acceptance criteria.

## Depends on

All of `tasks/001` through `tasks/008` (this is the integration point — it wires everything else
together).

## Files touched

- `application/controller/TransactionController.java` — add `PATCH`/`GET list`/`GET by id`
  handlers using `@CurrentOwnerId`, same pattern as the existing `POST` handler
- `application/mapper/TransactionResponseMapper.java` — confirm it already covers list/get
  response shapes (it should, if it maps `Transaction` → `TransactionResponse` generically)
- `infrastructure/config/MongoIndexConfig.java` — add every index from `data-model.md`'s two
  tables (`transactions` — already listed in the pre-existing §10 code block — plus the new
  `recurring_transaction_series` indexes: `{ownerId: 1, nextOccurrenceDate: 1}` and a unique index
  on `recurringSeriesId`)

## Acceptance Criteria

- **AC-T09-1:** `POST`, `GET` (list), `GET /{id}`, `PATCH /{id}` are all reachable over HTTP and
  dispatch to the correct handler; `DELETE /{id}` returns 404/405 (no route registered) — assert
  this explicitly in an integration test, since a missing route is easy to silently reintroduce
  later.
- **AC-T09-2:** Every index listed in `data-model.md` (both the `transactions` table and the new
  `recurring_transaction_series` table) exists at startup — extend whatever existing
  `MongoIndexConfig` test already verifies this for `003-accounts`, don't invent a new test
  pattern.
- **AC-T09-3:** OpenAPI contract (`contracts/transaction.openapi.yaml`) matches the controller
  exactly — no `deleteScope`/`editScope` anywhere, `PATCH` request schema limited to
  `description`/`categoryId`/`notes`/`tags`.

## Notes

This task is the integration checkpoint for the whole feature — treat its completion as the
"004-transactions is done" milestone, not any individual handler task.
