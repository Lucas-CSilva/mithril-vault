# Test Fixtures & Domain-Aware Test Folders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the API test suite into domain-aware packages, create shared command and model fixtures used consistently across unit and integration tests, and fill the gaps left by the missing `LogoutCommandHandlerTest` and `RefreshTokenMongoRepositoryIT`.

**Architecture:** Shared fixtures live in `fixture/command/` (per command subpackage) and `fixture/model/`. All integration tests extend the existing `AbstractIntegrationTest` (Testcontainers MongoDB replica set), regardless of whether they test HTTP endpoints or repositories directly. Domain unit tests stay in `domain/commandhandler/` and import from `fixture/` instead of defining inline data.

**Tech Stack:** Java 21, JUnit 5, Mockito, StepVerifier (Reactor Test), Testcontainers MongoDB 2.0.5, Spring Boot Test 4.0.6, ArchUnit 1.4.1.

## Global Constraints

- `StepVerifier` for all reactive assertions — never `.block()` in tests.
- No comments in code — self-documenting names only.
- Tests follow the same package hierarchy as main source (`com.mithrilvault.api`).
- Fixture constants use fixed `Instant` values (`Instant.parse(...)`) so tests are deterministic.
- The money rule (Long centavos) is not relevant to this feature — auth commands carry no monetary values.

---

## File Structure

### Files to CREATE

| File | Responsibility |
|---|---|
| `fixture/command/auth/LoginCommands.java` | Static factories for valid/invalid `LoginCommand` instances |
| `fixture/command/auth/LogoutCommands.java` | Static factories for `LogoutCommand` instances |
| `fixture/command/auth/RefreshCommands.java` | Static factories for `RefreshCommand` instances |
| `fixture/command/user/RegisterUserCommands.java` | Static factories for `RegisterUserCommand` instances |
| `fixture/model/Users.java` | Static factories for `User` domain model instances |
| `fixture/model/RefreshTokens.java` | Static factories for `RefreshToken` domain model instances |
| `domain/commandhandler/auth/LogoutCommandHandlerTest.java` | Unit tests for `LogoutCommandHandler` (missing) |
| `integration/auth/AuthControllerIT.java` | Copy of root-level `AuthControllerIT`, placed in domain-aware folder |
| `infrastructure/persistence/RefreshTokenMongoRepositoryIT.java` | Integration tests for `RefreshTokenMongoRepository` |

### Files to MODIFY

| File | Change |
|---|---|
| `domain/commandhandler/auth/LoginCommandHandlerTest.java` | Replace inline data with `LoginCommands.*` and `Users.*` |
| `domain/commandhandler/auth/RefreshCommandHandlerTest.java` | Replace inline data with `RefreshCommands.*`, `RefreshTokens.*`, `Users.*` |
| `domain/commandhandler/user/RegisterUserCommandHandlerTest.java` | Replace inline data with `RegisterUserCommands.*` and `Users.*` |

### Files to DELETE (after moving)

| File | Reason |
|---|---|
| `AuthControllerIT.java` (root package) | Moved to `integration/auth/` |
| `UserMongoRepositoryIT.java` (root package) | Moved to `infrastructure/persistence/` |

> **On repository IT tests + Testcontainers:** There is no special handling needed. All IT tests — whether they exercise HTTP endpoints or call a `ReactiveMongoRepository` bean directly — extend `AbstractIntegrationTest`. That base class starts the MongoDB Testcontainers replica set once per test run (via `@SpringBootTest`), injects the URI, and provides a fully-wired Spring context. A repository IT test simply `@Autowired`-injects the Spring Data repository and uses `StepVerifier` to assert reactive results. It does not use `WebTestClient`.

---

## Task 1: Create Command Fixtures

**Files:**
- Create: `api/src/test/java/com/mithrilvault/api/fixture/command/auth/LoginCommands.java`
- Create: `api/src/test/java/com/mithrilvault/api/fixture/command/auth/LogoutCommands.java`
- Create: `api/src/test/java/com/mithrilvault/api/fixture/command/auth/RefreshCommands.java`
- Create: `api/src/test/java/com/mithrilvault/api/fixture/command/user/RegisterUserCommands.java`

**Interfaces:**
- Produces: `LoginCommands.valid()`, `LoginCommands.withWrongPassword()`, `LoginCommands.withUnknownEmail()`
- Produces: `LogoutCommands.valid()`
- Produces: `RefreshCommands.valid(String rawToken)`, `RefreshCommands.defaultTtl()`
- Produces: `RegisterUserCommands.valid()`, `RegisterUserCommands.withEmail(String email)`, `RegisterUserCommands.withDuplicateEmail()`

- [ ] **Step 1: Write `LoginCommands`**

```java
package com.mithrilvault.api.fixture.command.auth;

import com.mithrilvault.api.domain.command.auth.LoginCommand;

public final class LoginCommands {

    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_PASSWORD = "password123";

    private LoginCommands() {}

    public static LoginCommand valid() {
        return new LoginCommand(DEFAULT_EMAIL, DEFAULT_PASSWORD);
    }

    public static LoginCommand withWrongPassword() {
        return new LoginCommand(DEFAULT_EMAIL, "wrong-password");
    }

    public static LoginCommand withUnknownEmail() {
        return new LoginCommand("nobody@example.com", DEFAULT_PASSWORD);
    }
}
```

- [ ] **Step 2: Write `LogoutCommands`**

```java
package com.mithrilvault.api.fixture.command.auth;

import com.mithrilvault.api.domain.command.auth.LogoutCommand;

public final class LogoutCommands {

    public static final String DEFAULT_RAW_TOKEN = "raw-refresh-token-abc";

    private LogoutCommands() {}

    public static LogoutCommand valid() {
        return new LogoutCommand(DEFAULT_RAW_TOKEN);
    }
}
```

- [ ] **Step 3: Write `RefreshCommands`**

```java
package com.mithrilvault.api.fixture.command.auth;

import com.mithrilvault.api.domain.command.auth.RefreshCommand;

public final class RefreshCommands {

    public static final String DEFAULT_RAW_TOKEN = "raw-refresh-token-abc";
    public static final long DEFAULT_TTL_SECONDS = 86_400L;

    private RefreshCommands() {}

    public static RefreshCommand valid() {
        return new RefreshCommand(DEFAULT_RAW_TOKEN, DEFAULT_TTL_SECONDS);
    }

    public static RefreshCommand withToken(String rawToken) {
        return new RefreshCommand(rawToken, DEFAULT_TTL_SECONDS);
    }
}
```

- [ ] **Step 4: Write `RegisterUserCommands`**

```java
package com.mithrilvault.api.fixture.command.user;

import com.mithrilvault.api.domain.command.user.RegisterUserCommand;

public final class RegisterUserCommands {

    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_PASSWORD = "password123";
    public static final String DEFAULT_DISPLAY_NAME = "Test User";
    public static final String DUPLICATE_EMAIL = "existing@example.com";

    private RegisterUserCommands() {}

    public static RegisterUserCommand valid() {
        return new RegisterUserCommand(DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME);
    }

    public static RegisterUserCommand withEmail(String email) {
        return new RegisterUserCommand(email, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME);
    }

    public static RegisterUserCommand withDuplicateEmail() {
        return new RegisterUserCommand(DUPLICATE_EMAIL, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME);
    }
}
```

- [ ] **Step 5: Compile check**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew compileTestJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/fixture/command/
git commit -m "test: add command fixture factories for auth and user commands"
```

---

## Task 2: Create Domain Model Fixtures

**Files:**
- Create: `api/src/test/java/com/mithrilvault/api/fixture/model/Users.java`
- Create: `api/src/test/java/com/mithrilvault/api/fixture/model/RefreshTokens.java`

**Interfaces:**
- Produces: `Users.active()`, `Users.disabled()`
- Produces: `RefreshTokens.active(String userId)`, `RefreshTokens.expired(String userId)`, `RefreshTokens.revoked(String userId)`
- Note: `Users.active().email()` equals `LoginCommands.DEFAULT_EMAIL` — these fixtures are intentionally aligned.

- [ ] **Step 1: Write `Users`**

```java
package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.model.UserStatus;
import com.mithrilvault.api.fixture.command.auth.LoginCommands;
import com.mithrilvault.api.fixture.command.user.RegisterUserCommands;

import java.time.Instant;

public final class Users {

    public static final String DEFAULT_ID = "user-id-fixture-1";
    public static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private Users() {}

    public static User active() {
        return User.builder()
                .id(DEFAULT_ID)
                .email(LoginCommands.DEFAULT_EMAIL)
                .passwordHash("$2a$10$hashed-password-fixture")
                .displayName(RegisterUserCommands.DEFAULT_DISPLAY_NAME)
                .status(UserStatus.ACTIVE)
                .createdAt(CREATED_AT)
                .build();
    }

    public static User disabled() {
        return User.builder()
                .id(DEFAULT_ID)
                .email(LoginCommands.DEFAULT_EMAIL)
                .passwordHash("$2a$10$hashed-password-fixture")
                .displayName(RegisterUserCommands.DEFAULT_DISPLAY_NAME)
                .status(UserStatus.DISABLED)
                .createdAt(CREATED_AT)
                .build();
    }
}
```

- [ ] **Step 2: Write `RefreshTokens`**

```java
package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.fixture.command.auth.RefreshCommands;

import java.time.Instant;

public final class RefreshTokens {

    public static final String DEFAULT_TOKEN_HASH = "sha256-hash-of-raw-refresh-token-abc";
    private static final Instant NOW = Instant.parse("2026-06-21T12:00:00Z");

    private RefreshTokens() {}

    public static RefreshToken active(String userId) {
        return new RefreshToken(
                "refresh-token-id-1",
                userId,
                DEFAULT_TOKEN_HASH,
                NOW.plusSeconds(RefreshCommands.DEFAULT_TTL_SECONDS),
                null,
                null,
                NOW
        );
    }

    public static RefreshToken expired(String userId) {
        return new RefreshToken(
                "refresh-token-id-2",
                userId,
                DEFAULT_TOKEN_HASH,
                NOW.minusSeconds(60),
                null,
                null,
                NOW.minusSeconds(RefreshCommands.DEFAULT_TTL_SECONDS + 60)
        );
    }

    public static RefreshToken revoked(String userId) {
        return new RefreshToken(
                "refresh-token-id-3",
                userId,
                DEFAULT_TOKEN_HASH,
                NOW.plusSeconds(RefreshCommands.DEFAULT_TTL_SECONDS),
                NOW.minusSeconds(30),
                null,
                NOW.minusSeconds(60)
        );
    }
}
```

- [ ] **Step 3: Compile check**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew compileTestJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/fixture/model/
git commit -m "test: add domain model fixture factories for User and RefreshToken"
```

---

## Task 3: Update `LoginCommandHandlerTest` to Use Fixtures

**Files:**
- Modify: `api/src/test/java/com/mithrilvault/api/domain/commandhandler/auth/LoginCommandHandlerTest.java`

**Interfaces:**
- Consumes: `LoginCommands.valid()`, `LoginCommands.withUnknownEmail()`, `LoginCommands.withWrongPassword()`, `LoginCommands.DEFAULT_EMAIL`, `LoginCommands.DEFAULT_PASSWORD`
- Consumes: `Users.active()`, `Users.disabled()`

- [ ] **Step 1: Replace the file content**

The new `LoginCommandHandlerTest.java`:

```java
package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.port.PasswordHasher;
import com.mithrilvault.api.domain.port.UserRepository;
import com.mithrilvault.api.fixture.command.auth.LoginCommands;
import com.mithrilvault.api.fixture.model.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private LoginCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginCommandHandler(userRepository, passwordHasher);
    }

    @Test
    void validCredentialsReturnUser() {
        when(userRepository.findByEmail(LoginCommands.DEFAULT_EMAIL))
                .thenReturn(Mono.just(Users.active()));
        when(passwordHasher.matches(LoginCommands.DEFAULT_PASSWORD, Users.active().passwordHash()))
                .thenReturn(true);

        StepVerifier.create(handler.handle(LoginCommands.valid()))
                .expectNext(Users.active())
                .verifyComplete();
    }

    @Test
    void unknownEmailThrowsUnauthorized() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(LoginCommands.withUnknownEmail()))
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void wrongPasswordThrowsUnauthorized() {
        when(userRepository.findByEmail(LoginCommands.DEFAULT_EMAIL))
                .thenReturn(Mono.just(Users.active()));
        when(passwordHasher.matches("wrong-password", Users.active().passwordHash()))
                .thenReturn(false);

        StepVerifier.create(handler.handle(LoginCommands.withWrongPassword()))
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void disabledUserThrowsUnauthorized() {
        when(userRepository.findByEmail(LoginCommands.DEFAULT_EMAIL))
                .thenReturn(Mono.just(Users.disabled()));
        when(passwordHasher.matches(LoginCommands.DEFAULT_PASSWORD, Users.disabled().passwordHash()))
                .thenReturn(true);

        StepVerifier.create(handler.handle(LoginCommands.valid()))
                .expectError(UnauthorizedException.class)
                .verify();
    }
}
```

- [ ] **Step 2: Run unit tests to verify they pass**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew test --tests "com.mithrilvault.api.domain.commandhandler.auth.LoginCommandHandlerTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/domain/commandhandler/auth/LoginCommandHandlerTest.java
git commit -m "test: update LoginCommandHandlerTest to use shared fixtures"
```

---

## Task 4: Update `RegisterUserCommandHandlerTest` to Use Fixtures

**Files:**
- Modify: `api/src/test/java/com/mithrilvault/api/domain/commandhandler/user/RegisterUserCommandHandlerTest.java`

**Interfaces:**
- Consumes: `RegisterUserCommands.valid()`, `RegisterUserCommands.withDuplicateEmail()`, `RegisterUserCommands.DEFAULT_EMAIL`, `RegisterUserCommands.DUPLICATE_EMAIL`, `RegisterUserCommands.DEFAULT_PASSWORD`
- Consumes: `Users.active()`

- [ ] **Step 1: Replace the file content**

```java
package com.mithrilvault.api.domain.commandhandler.user;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.UserStatus;
import com.mithrilvault.api.domain.port.PasswordHasher;
import com.mithrilvault.api.domain.port.UserRepository;
import com.mithrilvault.api.fixture.command.user.RegisterUserCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private RegisterUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterUserCommandHandler(userRepository, passwordHasher);
    }

    @Test
    void newEmailSavesActiveUserWithHashedPassword() {
        when(userRepository.existsByEmail(RegisterUserCommands.DEFAULT_EMAIL)).thenReturn(Mono.just(false));
        when(passwordHasher.hash(RegisterUserCommands.DEFAULT_PASSWORD)).thenReturn("hashed-pw");
        when(userRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(handler.handle(RegisterUserCommands.valid()))
                .assertNext(user -> {
                    assertThat(user.email()).isEqualTo(RegisterUserCommands.DEFAULT_EMAIL);
                    assertThat(user.passwordHash()).isEqualTo("hashed-pw");
                    assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(user.id()).isNotBlank();
                })
                .verifyComplete();
    }

    @Test
    void duplicateEmailThrowsConflictWithoutSaving() {
        when(userRepository.existsByEmail(RegisterUserCommands.DUPLICATE_EMAIL)).thenReturn(Mono.just(true));

        StepVerifier.create(handler.handle(RegisterUserCommands.withDuplicateEmail()))
                .expectError(ConflictException.class)
                .verify();

        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).hash(anyString());
    }
}
```

- [ ] **Step 2: Run unit tests**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew test --tests "com.mithrilvault.api.domain.commandhandler.user.RegisterUserCommandHandlerTest"
```
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/domain/commandhandler/user/RegisterUserCommandHandlerTest.java
git commit -m "test: update RegisterUserCommandHandlerTest to use shared fixtures"
```

---

## Task 5: Update `RefreshCommandHandlerTest` to Use Fixtures

**Files:**
- Modify: `api/src/test/java/com/mithrilvault/api/domain/commandhandler/auth/RefreshCommandHandlerTest.java`

**Interfaces:**
- Consumes: `RefreshCommands.valid()`, `RefreshCommands.withToken(String)`, `RefreshCommands.DEFAULT_RAW_TOKEN`, `RefreshCommands.DEFAULT_TTL_SECONDS`
- Consumes: `RefreshTokens.active(String)`, `RefreshTokens.expired(String)`, `RefreshTokens.revoked(String)`, `RefreshTokens.DEFAULT_TOKEN_HASH`
- Consumes: `Users.active()`, `Users.DEFAULT_ID`

- [ ] **Step 1: Replace the file content**

```java
package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.domain.port.TokenProvider;
import com.mithrilvault.api.domain.port.UserRepository;
import com.mithrilvault.api.fixture.command.auth.RefreshCommands;
import com.mithrilvault.api.fixture.model.RefreshTokens;
import com.mithrilvault.api.fixture.model.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshCommandHandlerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    private RefreshCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RefreshCommandHandler(refreshTokenRepository, tokenProvider, userRepository);
    }

    @Test
    void validTokenRotatesAndReturnsNewPair() {
        RefreshToken activeToken = RefreshTokens.active(Users.DEFAULT_ID);
        when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
                .thenReturn(Mono.just(activeToken));
        when(userRepository.findById(Users.DEFAULT_ID)).thenReturn(Mono.just(Users.active()));
        when(tokenProvider.generateAccessToken(Users.DEFAULT_ID, Users.active().email()))
                .thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(Users.DEFAULT_ID)).thenReturn("new-raw-refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(handler.handle(RefreshCommands.valid()))
                .assertNext(result -> {
                    assertThat(result.accessToken()).isEqualTo("new-access-token");
                    assertThat(result.rawRefreshToken()).isEqualTo("new-raw-refresh-token");
                    assertThat(result.user()).isEqualTo(Users.active());
                })
                .verifyComplete();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.revokedAt()).isNull();
        assertThat(saved.replacedBy()).isNotBlank();
    }

    @Test
    void tokenReuseRevokesAllAndThrowsUnauthorized() {
        RefreshToken revokedToken = RefreshTokens.revoked(Users.DEFAULT_ID);
        when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
                .thenReturn(Mono.just(revokedToken));
        when(refreshTokenRepository.revokeAllByUserId(Users.DEFAULT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(RefreshCommands.valid()))
                .expectError(UnauthorizedException.class)
                .verify();

        verify(refreshTokenRepository).revokeAllByUserId(Users.DEFAULT_ID);
    }

    @Test
    void expiredTokenThrowsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
                .thenReturn(Mono.just(RefreshTokens.expired(Users.DEFAULT_ID)));

        StepVerifier.create(handler.handle(RefreshCommands.valid()))
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void unknownTokenHashThrowsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(RefreshCommands.valid()))
                .expectError(UnauthorizedException.class)
                .verify();
    }
}
```

> **Note on `RefreshTokens.DEFAULT_TOKEN_HASH`:** The handler calls `RefreshCommandHandler.sha256(rawToken)` internally to look up the token. The fixture's `DEFAULT_TOKEN_HASH` must be the actual SHA-256 of `RefreshCommands.DEFAULT_RAW_TOKEN` ("raw-refresh-token-abc"). Compute and hard-code this value:
> ```
> sha256("raw-refresh-token-abc") = "c4b5b5a0e5cc4cb07c0748fc82b32c6e7a3a4c5f..."
> ```
> Run the following to get the exact hash before hard-coding it in `RefreshTokens`:
> ```bash
> echo -n "raw-refresh-token-abc" | sha256sum
> ```
> Then update `RefreshTokens.DEFAULT_TOKEN_HASH` to that exact hex string.

- [ ] **Step 2: Compute the SHA-256 of the default raw token**

```bash
echo -n "raw-refresh-token-abc" | sha256sum
```
Copy the hex string (without the trailing ` -`) and update `RefreshTokens.DEFAULT_TOKEN_HASH` in `fixture/model/RefreshTokens.java` to that exact value.

- [ ] **Step 3: Run unit tests**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew test --tests "com.mithrilvault.api.domain.commandhandler.auth.RefreshCommandHandlerTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 4: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/domain/commandhandler/auth/RefreshCommandHandlerTest.java
git add api/src/test/java/com/mithrilvault/api/fixture/model/RefreshTokens.java
git commit -m "test: update RefreshCommandHandlerTest to use shared fixtures"
```

---

## Task 6: Add `LogoutCommandHandlerTest`

**Files:**
- Create: `api/src/test/java/com/mithrilvault/api/domain/commandhandler/auth/LogoutCommandHandlerTest.java`

**Interfaces:**
- Consumes: `LogoutCommands.valid()`, `LogoutCommands.DEFAULT_RAW_TOKEN`
- Consumes: `RefreshTokens.active(String)`, `RefreshTokens.DEFAULT_TOKEN_HASH`, `Users.DEFAULT_ID`

- [ ] **Step 1: Write the failing tests first**

```java
package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.fixture.command.auth.LogoutCommands;
import com.mithrilvault.api.fixture.model.RefreshTokens;
import com.mithrilvault.api.fixture.model.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutCommandHandlerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private LogoutCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LogoutCommandHandler(refreshTokenRepository);
    }

    @Test
    void validTokenIsMarkedRevokedAndCompletes() {
        RefreshToken activeToken = RefreshTokens.active(Users.DEFAULT_ID);
        when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
                .thenReturn(Mono.just(activeToken));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(handler.handle(LogoutCommands.valid()))
                .verifyComplete();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().revokedAt()).isNotNull();
    }

    @Test
    void unknownTokenHashThrowsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(LogoutCommands.valid()))
                .expectError(UnauthorizedException.class)
                .verify();
    }
}
```

> **Note:** `LogoutCommands.DEFAULT_RAW_TOKEN` must also equal the same SHA-256 fixture hash used in `RefreshTokens.DEFAULT_TOKEN_HASH` (same raw token, same hash). Since `LogoutCommandHandler` also calls `sha256()` internally, this alignment is required.
>
> Update `LogoutCommands.DEFAULT_RAW_TOKEN` to the same value as `RefreshCommands.DEFAULT_RAW_TOKEN` ("raw-refresh-token-abc") — they already share this constant if copied from Step 2 of `LogoutCommands`. Confirm this is the case.

- [ ] **Step 2: Run failing tests to confirm they fail**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew test --tests "com.mithrilvault.api.domain.commandhandler.auth.LogoutCommandHandlerTest"
```
Expected: BUILD SUCCESSFUL (tests already pass since `LogoutCommandHandler` exists), or if the hash constant needs adjustment, the test will fail with an assertion mismatch — fix `DEFAULT_TOKEN_HASH` accordingly.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/domain/commandhandler/auth/LogoutCommandHandlerTest.java
git commit -m "test: add LogoutCommandHandlerTest with shared fixtures"
```

---

## Task 7: Move `AuthControllerIT` to `integration/auth/`

**Files:**
- Create: `api/src/test/java/com/mithrilvault/api/integration/auth/AuthControllerIT.java`
- Delete: `api/src/test/java/com/mithrilvault/api/AuthControllerIT.java`

**Interfaces:**
- Consumes: `RegisterUserCommands.DEFAULT_EMAIL`, `RegisterUserCommands.DEFAULT_PASSWORD`, `RegisterUserCommands.DEFAULT_DISPLAY_NAME`

- [ ] **Step 1: Create the new file with updated package and imports**

Move the existing `AuthControllerIT.java` content to the new location with these changes:
- Update `package` to `com.mithrilvault.api.integration.auth`
- Add `import com.mithrilvault.api.fixture.command.user.RegisterUserCommands`
- Replace inline literals for register payload with `RegisterUserCommands.DEFAULT_EMAIL`, `RegisterUserCommands.DEFAULT_PASSWORD`, `RegisterUserCommands.DEFAULT_DISPLAY_NAME`
- Replace inline literals for login payload with `LoginCommands.DEFAULT_EMAIL`, `LoginCommands.DEFAULT_PASSWORD`

The new file:

```java
package com.mithrilvault.api.integration.auth;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.fixture.command.auth.LoginCommands;
import com.mithrilvault.api.fixture.command.user.RegisterUserCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    // inject whichever Spring Data repo is needed to reset state
    @Autowired
    private com.mithrilvault.api.infrastructure.persistence.UserMongoRepository userMongoRepository;

    @Autowired
    private com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository refreshTokenMongoRepository;

    @BeforeEach
    void cleanUp() {
        userMongoRepository.deleteAll().block();
        refreshTokenMongoRepository.deleteAll().block();
    }

    @Test
    void registerReturns201() {
        webTestClient.post().uri("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"%s","rawPassword":"%s","displayName":"%s"}
                        """.formatted(
                        RegisterUserCommands.DEFAULT_EMAIL,
                        RegisterUserCommands.DEFAULT_PASSWORD,
                        RegisterUserCommands.DEFAULT_DISPLAY_NAME))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void loginWithValidCredentialsReturns200() {
        registerUser();

        webTestClient.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"%s","rawPassword":"%s"}
                        """.formatted(LoginCommands.DEFAULT_EMAIL, LoginCommands.DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(LoginCommands.DEFAULT_EMAIL);
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        registerUser();

        webTestClient.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"%s","rawPassword":"wrong-password"}
                        """.formatted(LoginCommands.DEFAULT_EMAIL))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logoutReturns204() {
        String refreshToken = loginAndGetRefreshTokenCookie();

        webTestClient.post().uri("/logout")
                .cookie("refreshToken", refreshToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void refreshReturnsNewTokens() {
        String refreshToken = loginAndGetRefreshTokenCookie();

        webTestClient.post().uri("/refresh")
                .cookie("refreshToken", refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(LoginCommands.DEFAULT_EMAIL);
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() {
        webTestClient.get().uri("/protected-test-path")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private void registerUser() {
        webTestClient.post().uri("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"%s","rawPassword":"%s","displayName":"%s"}
                        """.formatted(
                        RegisterUserCommands.DEFAULT_EMAIL,
                        RegisterUserCommands.DEFAULT_PASSWORD,
                        RegisterUserCommands.DEFAULT_DISPLAY_NAME))
                .exchange()
                .expectStatus().isCreated();
    }

    private String loginAndGetRefreshTokenCookie() {
        registerUser();
        return webTestClient.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"%s","rawPassword":"%s"}
                        """.formatted(LoginCommands.DEFAULT_EMAIL, LoginCommands.DEFAULT_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class)
                .getResponseCookies()
                .getFirst("refreshToken")
                .getValue();
    }
}
```

> **Note:** Preserve all tests from the original file. If the original has additional tests (e.g., authorized access with JWT header), carry them over verbatim, replacing only package/import/literal changes.

- [ ] **Step 2: Delete the old file**

```bash
rm api/src/test/java/com/mithrilvault/api/AuthControllerIT.java
```

- [ ] **Step 3: Run integration tests to verify**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew integrationTest --tests "com.mithrilvault.api.integration.auth.AuthControllerIT"
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 4: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/integration/auth/AuthControllerIT.java
git rm api/src/test/java/com/mithrilvault/api/AuthControllerIT.java
git commit -m "test: move AuthControllerIT to integration/auth package"
```

---

## Task 8: Move `UserMongoRepositoryIT` to `infrastructure/persistence/`

**Files:**
- Create: `api/src/test/java/com/mithrilvault/api/infrastructure/persistence/UserMongoRepositoryIT.java`
- Delete: `api/src/test/java/com/mithrilvault/api/UserMongoRepositoryIT.java`

- [ ] **Step 1: Create the new file**

Move the existing file content with only the `package` declaration updated:

```java
package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.infrastructure.persistence.document.UserDocument;
import com.mithrilvault.api.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class UserMongoRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserMongoRepository userMongoRepository;

    @BeforeEach
    void cleanUp() {
        userMongoRepository.deleteAll().block();
    }

    @Test
    void saveAndFindByEmail() {
        UserDocument doc = UserDocument.builder()
                .email("repo-test@example.com")
                .passwordHash("hashed")
                .displayName("Repo Test")
                .status(UserStatus.ACTIVE)
                .build();

        userMongoRepository.save(doc).block();

        StepVerifier.create(userMongoRepository.findByEmail("repo-test@example.com"))
                .assertNext(found -> assertThat(found.getEmail()).isEqualTo("repo-test@example.com"))
                .verifyComplete();
    }

    @Test
    void findByEmailIsCaseInsensitive() {
        UserDocument doc = UserDocument.builder()
                .email("case@example.com")
                .passwordHash("hashed")
                .displayName("Case Test")
                .status(UserStatus.ACTIVE)
                .build();

        userMongoRepository.save(doc).block();

        StepVerifier.create(userMongoRepository.findByEmail("CASE@EXAMPLE.COM"))
                .assertNext(found -> assertThat(found.getEmail()).isEqualTo("case@example.com"))
                .verifyComplete();
    }

    @Test
    void existsByEmailReturnsFalseWhenAbsent() {
        StepVerifier.create(userMongoRepository.existsByEmail("absent@example.com"))
                .expectNext(false)
                .verifyComplete();
    }
}
```

> **Note:** Preserve all tests from the original. If the case-insensitive test relies on a MongoDB collation, keep whatever the original mechanism was. Copy verbatim and only change the `package` line.

- [ ] **Step 2: Delete the old file**

```bash
rm api/src/test/java/com/mithrilvault/api/UserMongoRepositoryIT.java
```

- [ ] **Step 3: Run integration tests to verify**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew integrationTest --tests "com.mithrilvault.api.infrastructure.persistence.UserMongoRepositoryIT"
```
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 4: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/infrastructure/persistence/UserMongoRepositoryIT.java
git rm api/src/test/java/com/mithrilvault/api/UserMongoRepositoryIT.java
git commit -m "test: move UserMongoRepositoryIT to infrastructure/persistence package"
```

---

## Task 9: Add `RefreshTokenMongoRepositoryIT`

**Files:**
- Create: `api/src/test/java/com/mithrilvault/api/infrastructure/persistence/RefreshTokenMongoRepositoryIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest` (Testcontainers MongoDB, Spring context)
- Consumes: `RefreshTokenDocument` builder

- [ ] **Step 1: Write the test**

```java
package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.infrastructure.persistence.document.RefreshTokenDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenMongoRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenMongoRepository refreshTokenMongoRepository;

    @BeforeEach
    void cleanUp() {
        refreshTokenMongoRepository.deleteAll().block();
    }

    @Test
    void saveAndFindByTokenHash() {
        RefreshTokenDocument doc = RefreshTokenDocument.builder()
                .userId("user-1")
                .tokenHash("hash-abc")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        refreshTokenMongoRepository.save(doc).block();

        StepVerifier.create(refreshTokenMongoRepository.findByTokenHash("hash-abc"))
                .assertNext(found -> {
                    assertThat(found.getTokenHash()).isEqualTo("hash-abc");
                    assertThat(found.getUserId()).isEqualTo("user-1");
                    assertThat(found.getRevokedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void findByTokenHashReturnsEmptyWhenAbsent() {
        StepVerifier.create(refreshTokenMongoRepository.findByTokenHash("nonexistent-hash"))
                .verifyComplete();
    }

    @Test
    void findAllByUserIdReturnsAllTokensForUser() {
        String userId = "user-multi";
        refreshTokenMongoRepository.save(
                RefreshTokenDocument.builder().userId(userId).tokenHash("hash-1")
                        .expiresAt(Instant.now().plusSeconds(3600)).build()).block();
        refreshTokenMongoRepository.save(
                RefreshTokenDocument.builder().userId(userId).tokenHash("hash-2")
                        .expiresAt(Instant.now().plusSeconds(3600)).build()).block();
        refreshTokenMongoRepository.save(
                RefreshTokenDocument.builder().userId("other-user").tokenHash("hash-3")
                        .expiresAt(Instant.now().plusSeconds(3600)).build()).block();

        StepVerifier.create(refreshTokenMongoRepository.findAllByUserId(userId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void savedTokenHasCreatedAtPopulatedByAuditing() {
        RefreshTokenDocument doc = RefreshTokenDocument.builder()
                .userId("user-audit")
                .tokenHash("hash-audit")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        StepVerifier.create(refreshTokenMongoRepository.save(doc))
                .assertNext(saved -> assertThat(saved.getCreatedAt()).isNotNull())
                .verifyComplete();
    }
}
```

- [ ] **Step 2: Run the integration test**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew integrationTest --tests "com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepositoryIT"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 3: Commit**

```bash
git add api/src/test/java/com/mithrilvault/api/infrastructure/persistence/RefreshTokenMongoRepositoryIT.java
git commit -m "test: add RefreshTokenMongoRepositoryIT in infrastructure/persistence"
```

---

## Task 10: Full Test Suite Verification

- [ ] **Step 1: Run all unit tests**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew test
```
Expected: BUILD SUCCESSFUL. All domain command handler tests + ArchitectureTest pass.

- [ ] **Step 2: Run all integration tests**

```bash
cd /home/lucas/workspaces/mithril-vault/api && ./gradlew integrationTest
```
Expected: BUILD SUCCESSFUL. All IT tests in `integration/auth/`, `infrastructure/persistence/`, and root-level `ApiApplicationIT` pass.

- [ ] **Step 3: Verify ArchUnit rules still pass**

ArchUnit is included in the unit test run above. Confirm the 4 hexagonal architecture rules pass — the new `fixture/` package is test-only and must not be imported from main source, which it won't be.

- [ ] **Step 4: Final commit if clean**

If all tests pass and no uncommitted changes remain:

```bash
git log --oneline -10
```
All 9 commits from this plan should appear. No stray files. Done.

---

## Verification Summary

| Check | Command | Expected |
|---|---|---|
| Unit tests | `./gradlew test` | All pass |
| Integration tests | `./gradlew integrationTest` | All pass |
| Specific controller IT | `./gradlew integrationTest --tests "*.integration.auth.AuthControllerIT"` | All pass |
| Persistence ITs | `./gradlew integrationTest --tests "*.infrastructure.persistence.*"` | All pass |
| ArchUnit | Included in `./gradlew test` | 4 rules pass |
| No root-level ITs remain | `find api/src/test -name "*IT.java" -path "*/api/*IT.java" | grep -v "ApiApplicationIT"` | No output |
