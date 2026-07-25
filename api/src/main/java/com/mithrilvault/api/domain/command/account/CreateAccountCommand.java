package com.mithrilvault.api.domain.command.account;

import com.mithrilvault.api.domain.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateAccountCommand(
    @NotBlank @Size(max = 100) String name,
    @NotNull AccountType type,
    @Size(max = 100) String institution,
    @NotNull @PositiveOrZero Long initialBalance,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {}
