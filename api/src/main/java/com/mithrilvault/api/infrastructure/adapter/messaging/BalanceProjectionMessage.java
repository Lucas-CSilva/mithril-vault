package com.mithrilvault.api.infrastructure.adapter.messaging;

import com.mithrilvault.api.domain.model.TransactionType;
import lombok.Builder;

@Builder
public record BalanceProjectionMessage(
    String id,
    String ownerId,
    String transactionId,
    String accountId,
    String invoiceId,
    TransactionType type,
    Long amount,
    ProjectionTarget target) {}
