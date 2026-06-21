package com.mithrilvault.api.domain.commandhandler.auth;

import com.mithrilvault.api.domain.command.auth.RefreshCommand;
import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.domain.port.TokenProvider;
import com.mithrilvault.api.domain.port.UserRepository;
import com.mithrilvault.api.domain.result.IssuedTokens;
import com.mithrilvault.api.domain.service.RefreshTokenService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RefreshCommandHandler {

  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenProvider tokenProvider;
  private final UserRepository userRepository;
  private final RefreshTokenService refreshTokenService;

  public Mono<IssuedTokens> handle(RefreshCommand command) {
    String incomingHash = refreshTokenService.hash(command.rawRefreshToken());

    return refreshTokenRepository
        .findByTokenHash(incomingHash)
        .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid refresh token")))
        .flatMap(
            token -> {
              if (token.revokedAt() != null) {
                return refreshTokenRepository
                    .revokeAllByUserId(token.userId())
                    .then(Mono.error(new UnauthorizedException("Token reused")));
              }

              if (token.expiresAt().isBefore(Instant.now())) {
                return Mono.error(new UnauthorizedException("Token expired"));
              }

              return userRepository
                  .findById(token.userId())
                  .switchIfEmpty(Mono.error(new UnauthorizedException("User not found")))
                  .flatMap(
                      user -> {
                        String newAccessToken =
                            tokenProvider.generateAccessToken(user.id(), user.email());
                        return refreshTokenService
                            .rotate(token, user)
                            .map(
                                rawRefreshToken ->
                                    new IssuedTokens(newAccessToken, rawRefreshToken, user));
                      });
            });
  }
}
