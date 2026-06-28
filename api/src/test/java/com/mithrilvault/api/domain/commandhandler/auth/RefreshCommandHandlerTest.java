package com.mithrilvault.api.domain.commandhandler.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.domain.port.TokenProvider;
import com.mithrilvault.api.domain.port.UserRepository;
import com.mithrilvault.api.domain.result.IssuedTokens;
import com.mithrilvault.api.domain.service.RefreshTokenService;
import com.mithrilvault.api.fixture.command.auth.RefreshCommands;
import com.mithrilvault.api.fixture.model.RefreshTokens;
import com.mithrilvault.api.fixture.model.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RefreshCommandHandlerTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private TokenProvider tokenProvider;
  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenService refreshTokenService;

  private RefreshCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new RefreshCommandHandler(
            refreshTokenRepository, tokenProvider, userRepository, refreshTokenService);
    when(refreshTokenService.hash(RefreshCommands.DEFAULT_RAW_TOKEN))
        .thenReturn(RefreshTokens.DEFAULT_TOKEN_HASH);
  }

  @Test
  void validTokenRotatesAndReturnsNewPair() {
    RefreshToken activeToken = RefreshTokens.active(Users.DEFAULT_ID);
    when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
        .thenReturn(Mono.just(activeToken));
    when(userRepository.findById(Users.DEFAULT_ID)).thenReturn(Mono.just(Users.active()));
    when(tokenProvider.generateAccessToken(Users.DEFAULT_ID, Users.active().email()))
        .thenReturn("new-access-token");
    when(refreshTokenService.rotate(activeToken, Users.active()))
        .thenReturn(Mono.just("new-raw-refresh-token"));

    StepVerifier.create(handler.handle(RefreshCommands.valid()))
        .assertNext(
            (IssuedTokens result) -> {
              assertThat(result.accessToken()).isEqualTo("new-access-token");
              assertThat(result.rawRefreshToken()).isEqualTo("new-raw-refresh-token");
              assertThat(result.user()).isEqualTo(Users.active());
            })
        .verifyComplete();

    verify(refreshTokenService).rotate(activeToken, Users.active());
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
