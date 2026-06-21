package com.mithrilvault.api.fixture.command.auth;

import com.mithrilvault.api.domain.command.auth.LogoutCommand;

public final class LogoutCommands {

    public static final String DEFAULT_RAW_TOKEN = "raw-refresh-token-abc";

    private LogoutCommands() {}

    public static LogoutCommand valid() {
        return new LogoutCommand(DEFAULT_RAW_TOKEN);
    }
}
