package com.mithrilvault.api.fixture.command.auth;

import com.mithrilvault.api.domain.command.auth.RefreshCommand;

public final class RefreshCommands {

    public static final String DEFAULT_RAW_TOKEN = "raw-refresh-token-abc";
    public static final long DEFAULT_TTL_SECONDS = 86_400L;

    private RefreshCommands() {}

    public static RefreshCommand valid() {
        return new RefreshCommand(DEFAULT_RAW_TOKEN, DEFAULT_TTL_SECONDS);
    }

    public static RefreshCommand withToken(String rawToken) {
        return new RefreshCommand(rawToken, DEFAULT_TTL_SECONDS);
    }
}
