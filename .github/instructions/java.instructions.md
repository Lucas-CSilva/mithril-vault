---
applyTo: "**/*.java"
---
# Java Backend Engineering Guidelines

## Tech Stack & Environment
- **Java Version:** 21 (LTS).
- **Framework:** Spring Boot (WebFlux).
- **Database:** MongoDB (Reactive Driver).
- **Paradigm:** Reactive Programming (Project Reactor).

## 1. Java 21 & Modern Practices
- **Records:** Use `record` for all DTOs, Value Objects, and event messages.
- **Pattern Matching:** Utilize `switch` expressions and pattern matching for `instanceof`.
- **Var:** Use `var` only when the type is obvious from the right side of the assignment.
- **Optional:** Avoid returning `null`. Use `Optional<T>` for synchronous code, or `Mono<T>`/`Mono.empty()` for reactive flows.

## 2. Reactive Programming (Strict)
- **Non-Blocking:** NEVER use `.block()`, `.blockFirst()`, or `.blockLast()` in production code. Everything must remain in the reactive chain.
- **Chaining:** Prefer method references (`System.out::println`) over lambdas where possible for readability.
- **Error Handling:** Use operator-based error handling (`onErrorResume`, `onErrorMap`) instead of `try-catch` blocks.
- **Context:** Use `Mono.deferContextual` if you need to access context data (like strict traceability IDs).

## 3. Financial Domain Rules
- **Precision:** STRICTLY use `BigDecimal` for all monetary values.
    - NEVER use `double` or `float` for money.
    - Set explicit `RoundingMode` (usually `HALF_EVEN`) when dividing.
- **Immutability:** Financial transactions must be immutable. Once a transaction Record is created, it cannot be modified.

## 4. Spring Boot Architecture
- **Dependency Injection:** Mandatory Constructor Injection.
    - Use `final` fields.
    - Use `@RequiredArgsConstructor` (Lombok) or explicit constructors.
    - **FORBIDDEN:** Field injection (`@Autowired` on private fields).
- **Lombok Usage:**
    - Allowed: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`.
    - **FORBIDDEN:** `@Data` on `@Document` classes (causes performance issues with `hashCode`/`equals` loops).

## 5. Code Quality (Zero Comments Policy)
- **Self-Documenting:** Do not write comments.
    - If logic is complex, extract it into a private method with a highly descriptive name.
    - Variable names must describe the *content* and *purpose* (e.g., `monthlyInterestRate` is better than `rate`).
- **Testing:**
    - Use `StepVerifier` for all reactive stream tests.
    - Assertions must be fluent (AssertJ).

## 6. Project Structure (Mithril-Vault)
- **Controller:** Exposes endpoints, delegates to Service. Returns `Mono<ResponseEntity<T>>`.
- **Service:** Contains business logic. Returns `Mono<DomainObject>` or `Flux<DomainObject>`.
- **Repository:** Extends `ReactiveMongoRepository`.