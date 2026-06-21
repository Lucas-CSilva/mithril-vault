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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        List<RefreshToken> savedTokens = captor.getAllValues();

        RefreshToken replacedSave = savedTokens.stream()
                .filter(t -> t.id().equals(activeToken.id()))
                .findFirst()
                .orElseThrow();
        assertThat(replacedSave.revokedAt()).isNotNull();
        assertThat(replacedSave.replacedBy()).isNotBlank();

        RefreshToken newTokenSave = savedTokens.stream()
                .filter(t -> !t.id().equals(activeToken.id()))
                .findFirst()
                .orElseThrow();
        assertThat(newTokenSave.revokedAt()).isNull();
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
