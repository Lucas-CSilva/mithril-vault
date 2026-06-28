package com.mithrilvault.api.domain.commandhandler.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.command.auth.IssueTokensCommand;
import com.mithrilvault.api.domain.port.TokenProvider;
import com.mithrilvault.api.domain.result.IssuedTokens;
import com.mithrilvault.api.domain.service.RefreshTokenService;
import com.mithrilvault.api.fixture.model.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class IssueTokensCommandHandlerTest {

  @Mock private TokenProvider tokenProvider;
  @Mock private RefreshTokenService refreshTokenService;

  private IssueTokensCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new IssueTokensCommandHandler(tokenProvider, refreshTokenService);
  }

  @Test
  void issuesAccessAndRefreshTokensForUser() {
    var user = Users.active();
    when(tokenProvider.generateAccessToken(user.id(), user.email())).thenReturn("access-token");
    when(refreshTokenService.issue(user)).thenReturn(Mono.just("raw-refresh-token"));

    StepVerifier.create(handler.handle(new IssueTokensCommand(user)))
        .assertNext(
            (IssuedTokens result) -> {
              assertThat(result.accessToken()).isEqualTo("access-token");
              assertThat(result.rawRefreshToken()).isEqualTo("raw-refresh-token");
              assertThat(result.user()).isEqualTo(user);
            })
        .verifyComplete();
  }
}
