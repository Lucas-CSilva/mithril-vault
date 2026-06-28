package com.mithrilvault.api.domain.service;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.domain.port.TokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RefreshTokenService {

  private final AppProperties appProperties;
  private final TokenProvider tokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  public Mono<String> issue(User user) {
    return createAndSave(user).map(TokenPair::rawToken);
  }

  public Mono<String> rotate(RefreshToken oldToken, User user) {
    return createAndSave(user)
        .flatMap(
            newToken ->
                refreshTokenRepository
                    .save(oldToken.revoke(newToken.stored().tokenHash()))
                    .thenReturn(newToken.rawToken()));
  }

  public String hash(String input) {
    return DigestAlgorithm.SHA_256.hash(input.getBytes(StandardCharsets.UTF_8));
  }

  private Mono<TokenPair> createAndSave(User user) {
    String rawToken = tokenProvider.generateRefreshToken(user.id());
    RefreshToken stored =
        RefreshToken.builder()
            .userId(user.id())
            .tokenHash(hash(rawToken))
            .expiresAt(Instant.now().plusSeconds(appProperties.jwt().refreshTokenTtlSeconds()))
            .build();
    return refreshTokenRepository.save(stored).thenReturn(new TokenPair(rawToken, stored));
  }

  private record TokenPair(String rawToken, RefreshToken stored) {}

  private enum DigestAlgorithm {
    SHA_256("SHA-256");

    private final String id;

    DigestAlgorithm(String id) {
      this.id = id;
    }

    String hash(byte[] bytes) {
      try {
        return HexFormat.of().formatHex(MessageDigest.getInstance(id).digest(bytes));
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(id + " not available", e);
      }
    }
  }
}
