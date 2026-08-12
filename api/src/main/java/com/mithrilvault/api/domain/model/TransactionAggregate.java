package com.mithrilvault.api.domain.model;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransactionAggregate(Long balance, String lastTransactionId, Instant lastCreatedAt) {}
