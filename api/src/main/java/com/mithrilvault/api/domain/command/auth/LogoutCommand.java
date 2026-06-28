package com.mithrilvault.api.domain.command.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutCommand(@NotBlank String rawRefreshToken) {}
