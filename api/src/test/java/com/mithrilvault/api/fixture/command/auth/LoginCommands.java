package com.mithrilvault.api.fixture.command.auth;

import com.mithrilvault.api.domain.command.auth.LoginCommand;

public final class LoginCommands {

    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_PASSWORD = "password123";

    private LoginCommands() {}

    public static LoginCommand valid() {
        return new LoginCommand(DEFAULT_EMAIL, DEFAULT_PASSWORD);
    }

    public static LoginCommand withWrongPassword() {
        return new LoginCommand(DEFAULT_EMAIL, "wrong-password");
    }

    public static LoginCommand withUnknownEmail() {
        return new LoginCommand("nobody@example.com", DEFAULT_PASSWORD);
    }
}
