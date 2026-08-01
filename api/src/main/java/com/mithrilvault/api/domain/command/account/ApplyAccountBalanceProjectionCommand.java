package com.mithrilvault.api.domain.command.account;

import com.mithrilvault.api.domain.model.TransactionType;

public record ApplyAccountBalanceProjectionCommand(
    String ownerId,
    String transactionId,
    String accountId,
    TransactionType type,
    Long amount,
    String projectionId) {}
