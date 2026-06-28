package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.command.auth.IssueTokensCommand;
import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.port.TokenProvider;
import com.mithrilvault.api.domain.result.IssuedTokens;
import com.mithrilvault.api.domain.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class IssueTokensCommandHandler {

  private final TokenProvider tokenProvider;
  private final RefreshTokenService refreshTokenService;

  public Mono<IssuedTokens> handle(IssueTokensCommand command) {
    User user = command.user();
    String accessToken = tokenProvider.generateAccessToken(user.id(), user.email());
    return refreshTokenService
        .issue(user)
        .map(rawRefreshToken -> new IssuedTokens(accessToken, rawRefreshToken, user));
  }
}
