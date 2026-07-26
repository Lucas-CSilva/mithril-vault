package com.mithrilvault.api.domain.command.transaction;

import jakarta.validation.constraints.NotBlank;

public record TransferConfig(@NotBlank String destinationAccountId, String transferPairId) {}
