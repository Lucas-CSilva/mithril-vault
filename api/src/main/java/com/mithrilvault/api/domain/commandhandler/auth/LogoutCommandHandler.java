package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.command.auth.LogoutCommand;
import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LogoutCommandHandler {

  private final RefreshTokenRepository refreshTokenRepository;

  public Mono<Void> handle(LogoutCommand command) {
    String tokenHash = RefreshCommandHandler.sha256(command.rawRefreshToken());
    return refreshTokenRepository
        .findByTokenHash(tokenHash)
        .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid refresh token")))
        .flatMap(
            token -> {
              if (token.revokedAt() != null) {
                return Mono.empty();
              }
              RefreshToken revoked =
                  new RefreshToken(
                      token.id(),
                      token.userId(),
                      token.tokenHash(),
                      token.expiresAt(),
                      Instant.now(),
                      token.replacedBy(),
                      token.createdAt());
              return refreshTokenRepository.save(revoked).then();
            });
  }
}
