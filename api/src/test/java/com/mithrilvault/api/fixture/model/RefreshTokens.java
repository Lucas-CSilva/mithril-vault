package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.fixture.command.auth.RefreshCommands;

import java.time.Instant;

public final class RefreshTokens {

    public static final String DEFAULT_TOKEN_HASH = "sha256-hash-of-raw-refresh-token-abc";
    private static final Instant NOW = Instant.parse("2026-06-21T12:00:00Z");

    private RefreshTokens() {}

    public static RefreshToken active(String userId) {
        return new RefreshToken(
                "refresh-token-id-1",
                userId,
                DEFAULT_TOKEN_HASH,
                NOW.plusSeconds(RefreshCommands.DEFAULT_TTL_SECONDS),
                null,
                null,
                NOW
        );
    }

    public static RefreshToken expired(String userId) {
        return new RefreshToken(
                "refresh-token-id-2",
                userId,
                DEFAULT_TOKEN_HASH,
                NOW.minusSeconds(60),
                null,
                null,
                NOW.minusSeconds(RefreshCommands.DEFAULT_TTL_SECONDS + 60)
        );
    }

    public static RefreshToken revoked(String userId) {
        return new RefreshToken(
                "refresh-token-id-3",
                userId,
                DEFAULT_TOKEN_HASH,
                NOW.plusSeconds(RefreshCommands.DEFAULT_TTL_SECONDS),
                NOW.minusSeconds(30),
                null,
                NOW.minusSeconds(60)
        );
    }
}
