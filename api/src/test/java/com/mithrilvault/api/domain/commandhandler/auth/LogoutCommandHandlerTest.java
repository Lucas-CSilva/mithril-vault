package com.mithrilvault.api.domain.commandhandler.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.domain.service.RefreshTokenService;
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

@ExtendWith(MockitoExtension.class)
class LogoutCommandHandlerTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private RefreshTokenService refreshTokenService;

  private LogoutCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new LogoutCommandHandler(refreshTokenRepository, refreshTokenService);
    when(refreshTokenService.hash(LogoutCommands.DEFAULT_RAW_TOKEN))
        .thenReturn(RefreshTokens.DEFAULT_TOKEN_HASH);
  }

  @Test
  void validTokenIsMarkedRevokedAndCompletes() {
    RefreshToken activeToken = RefreshTokens.active(Users.DEFAULT_ID);
    when(refreshTokenRepository.findByTokenHash(RefreshTokens.DEFAULT_TOKEN_HASH))
        .thenReturn(Mono.just(activeToken));
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(handler.handle(LogoutCommands.valid())).verifyComplete();

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
