# Task 008 — CategorySuggestionService + suggest-category endpoint

## Scope

**In scope:** `CategorySuggestionService`, a pure function (no ports, no Spring stereotypes on the
domain type itself beyond what the codebase already allows) that maps a free-text description to a
suggested `categoryId` via static keyword matching, and the read-only
`GET /transactions/suggest-category` endpoint. No architectural change from the original spec.

**Out of scope:** any ML/NLP approach (explicitly out of scope per PRD-004 §2 Non-Goals);
auto-applying the suggestion — this is suggest-only, the client decides whether to use it.

## Depends on

Nothing.

## Files touched

- `domain/service/CategorySuggestionService.java` — new
- `domain/service/CategorySuggestionServiceTest.java` — new (table-driven over the keyword list)
- `application/controller/TransactionController.java` — add the `GET /suggest-category` route

## Acceptance Criteria

- **AC-T08-1:** A description containing a known keyword (per the table in
  `docs/product-definition.md` §Module 2) returns the matching `categoryId`.
- **AC-T08-2:** A description matching no keyword returns `null` (not an error, not a 404) — per
  the contract's `nullable: true` on the response.
- **AC-T08-3:** Matching is case-insensitive and accent-insensitive (normalize: uppercase, strip
  accents, per `implementation-notes.md` §8).
- **AC-T08-4:** The endpoint requires no request body, is a plain `GET`, and does not persist
  anything.

## Notes

Table-driven test means: one test method, one `@MethodSource`/`@CsvSource`-style table of
`(description, expectedCategoryId)` pairs pulled from the product-definition keyword table — not
one hand-written test per keyword.
