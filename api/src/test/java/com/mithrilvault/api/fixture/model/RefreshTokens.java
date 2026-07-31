package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.RefreshToken;
import java.time.Instant;

public final class RefreshTokens {

  public static final String DEFAULT_TOKEN_HASH =
      "22268f377c4f5fd99747308d325ae66f9c6e6fd1532af89dc5d2a782c0d370e2";
  private static final long TOKEN_TTL_SECONDS = 86_400L;
  private static final Instant NOW = Instant.now();

  private RefreshTokens() {}

  public static RefreshToken active(String userId) {
    return new RefreshToken(
        "refresh-token-id-1",
        userId,
        DEFAULT_TOKEN_HASH,
        NOW.plusSeconds(TOKEN_TTL_SECONDS),
        null,
        null,
        NOW,
        0L);
  }

  public static RefreshToken expired(String userId) {
    return new RefreshToken(
        "refresh-token-id-2",
        userId,
        DEFAULT_TOKEN_HASH,
        NOW.minusSeconds(60),
        null,
        null,
        NOW.minusSeconds(TOKEN_TTL_SECONDS + 60),
        0L);
  }

  public static RefreshToken revoked(String userId) {
    return new RefreshToken(
        "refresh-token-id-3",
        userId,
        DEFAULT_TOKEN_HASH,
        NOW.plusSeconds(TOKEN_TTL_SECONDS),
        NOW.minusSeconds(30),
        null,
        NOW.minusSeconds(60),
        0L);
  }

  public static RefreshToken newlyCreated(String userId) {
    return RefreshToken.builder()
        .userId(userId)
        .tokenHash("new-token-hash")
        .expiresAt(NOW.plusSeconds(TOKEN_TTL_SECONDS))
        .build();
  }
}
