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

        StepVerifier.create(handler.handle(LoginCommands.valid()))
                .expectError(UnauthorizedException.class)
                .verify();
    }
}
