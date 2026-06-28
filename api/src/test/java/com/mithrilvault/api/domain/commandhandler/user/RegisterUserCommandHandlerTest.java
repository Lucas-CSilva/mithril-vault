package com.mithrilvault.api.domain.commandhandler.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordHasher passwordHasher;

  private RegisterUserCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new RegisterUserCommandHandler(userRepository, passwordHasher);
  }

  @Test
  void newEmailSavesActiveUserWithHashedPassword() {
    when(userRepository.existsByEmail(RegisterUserCommands.DEFAULT_EMAIL))
        .thenReturn(Mono.just(false));
    when(passwordHasher.hash(RegisterUserCommands.DEFAULT_PASSWORD)).thenReturn("hashed-pw");
    when(userRepository.save(any()))
        .thenAnswer(
            inv -> {
              var user = (com.mithrilvault.api.domain.model.User) inv.getArgument(0);
              return Mono.just(
                  new com.mithrilvault.api.domain.model.User(
                      "id-123",
                      user.email(),
                      user.passwordHash(),
                      user.displayName(),
                      user.status(),
                      java.time.Instant.now()));
            });

    StepVerifier.create(handler.handle(RegisterUserCommands.valid()))
        .assertNext(
            user -> {
              assertThat(user.email()).isEqualTo(RegisterUserCommands.DEFAULT_EMAIL);
              assertThat(user.passwordHash()).isEqualTo("hashed-pw");
              assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
              assertThat(user.id()).isNotBlank();
            })
        .verifyComplete();
  }

  @Test
  void duplicateEmailThrowsConflictWithoutSaving() {
    when(userRepository.existsByEmail(RegisterUserCommands.DUPLICATE_EMAIL))
        .thenReturn(Mono.just(true));

    StepVerifier.create(handler.handle(RegisterUserCommands.withDuplicateEmail()))
        .expectError(ConflictException.class)
        .verify();

    verify(userRepository, never()).save(any());
    verify(passwordHasher, never()).hash(anyString());
  }
}
