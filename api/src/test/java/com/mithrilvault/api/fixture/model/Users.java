package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.model.UserStatus;
import com.mithrilvault.api.fixture.command.auth.LoginCommands;
import com.mithrilvault.api.fixture.command.user.RegisterUserCommands;

import java.time.Instant;

public final class Users {

    public static final String DEFAULT_ID = "user-id-fixture-1";
    public static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private Users() {}

    public static User active() {
        return User.builder()
                .id(DEFAULT_ID)
                .email(LoginCommands.DEFAULT_EMAIL)
                .passwordHash("$2a$10$hashed-password-fixture")
                .displayName(RegisterUserCommands.DEFAULT_DISPLAY_NAME)
                .status(UserStatus.ACTIVE)
                .createdAt(CREATED_AT)
                .build();
    }

    public static User disabled() {
        return User.builder()
                .id(DEFAULT_ID)
                .email(LoginCommands.DEFAULT_EMAIL)
                .passwordHash("$2a$10$hashed-password-fixture")
                .displayName(RegisterUserCommands.DEFAULT_DISPLAY_NAME)
                .status(UserStatus.DISABLED)
                .createdAt(CREATED_AT)
                .build();
    }
}
