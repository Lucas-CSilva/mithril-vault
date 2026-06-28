package com.mithrilvault.api.domain.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record RefreshToken(
    String id,
    String userId,
    String tokenHash,
    Instant expiresAt,
    Instant revokedAt,
    String replacedBy,
    Instant createdAt) {

  public RefreshToken revoke(String newTokenId) {
    return RefreshToken.builder()
        .id(this.id())
        .userId(this.userId())
        .tokenHash(this.tokenHash())
        .expiresAt(this.expiresAt())
        .revokedAt(Instant.now())
        .replacedBy(newTokenId)
        .createdAt(this.createdAt())
        .build();
  }

  public RefreshToken revoke() {
    return RefreshToken.builder()
        .id(this.id())
        .userId(this.userId())
        .tokenHash(this.tokenHash())
        .expiresAt(this.expiresAt())
        .revokedAt(Instant.now())
        .createdAt(this.createdAt())
        .build();
  }
}
