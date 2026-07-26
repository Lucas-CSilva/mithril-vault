package com.mithrilvault.api.domain.command.transaction;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InstallmentConfig(@NotNull @Min(2) @Max(48) Integer totalInstallments) {}
