package com.mithrilvault.api.domain.command.account;

import com.mithrilvault.api.domain.model.AccountType;
import jakarta.validation.constraints.*;

public record UpdateAccountCommand(
    @Size(max = 100) String name,
    AccountType type,
    @Size(max = 100) String institution,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {}
