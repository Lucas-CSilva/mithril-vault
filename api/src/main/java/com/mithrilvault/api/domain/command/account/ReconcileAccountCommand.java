package com.mithrilvault.api.domain.command.account;

import com.mithrilvault.api.domain.model.ReconciliationMethod;
import jakarta.validation.constraints.NotNull;

public record ReconcileAccountCommand(
    @NotNull Long realBalance, @NotNull ReconciliationMethod method) {}
