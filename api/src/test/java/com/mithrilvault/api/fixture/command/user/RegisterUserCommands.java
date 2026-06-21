package com.mithrilvault.api.fixture.command.user;

import com.mithrilvault.api.domain.command.user.RegisterUserCommand;

public final class RegisterUserCommands {

    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_PASSWORD = "password123";
    public static final String DEFAULT_DISPLAY_NAME = "Test User";
    public static final String DUPLICATE_EMAIL = "existing@example.com";

    private RegisterUserCommands() {}

    public static RegisterUserCommand valid() {
        return new RegisterUserCommand(DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME);
    }

    public static RegisterUserCommand withEmail(String email) {
        return new RegisterUserCommand(email, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME);
    }

    public static RegisterUserCommand withDuplicateEmail() {
        return new RegisterUserCommand(DUPLICATE_EMAIL, DEFAULT_PASSWORD, DEFAULT_DISPLAY_NAME);
    }
}
