package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.command.auth.LogoutCommand;
import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.domain.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LogoutCommandHandler {

  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenService refreshTokenService;

  public Mono<Void> handle(LogoutCommand command) {
    String tokenHash = refreshTokenService.hash(command.rawRefreshToken());
    return refreshTokenRepository
        .findByTokenHash(tokenHash)
        .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid refresh token")))
        .flatMap(
            token -> {
              if (token.revokedAt() != null) {
                return Mono.empty();
              }

              return refreshTokenRepository.save(token.revoke()).then();
            });
  }
}
