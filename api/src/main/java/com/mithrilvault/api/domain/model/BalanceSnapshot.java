package com.mithrilvault.api.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record BalanceSnapshot(
    String id,
    String ownerId,
    String accountId,
    LocalDate asOfDate,
    Long balance,
    String lastTransactionId,
    Instant lastCreatedAt,
    Instant createdAt,
    Long version) {

  public static BalanceSnapshot create(
      Account account, TransactionAggregate checkpoint, LocalDate asOfDate) {
    return BalanceSnapshot.builder()
        .ownerId(account.ownerId())
        .accountId(account.id())
        .asOfDate(asOfDate)
        .balance(checkpoint.balance())
        .lastTransactionId(checkpoint.lastTransactionId())
        .lastCreatedAt(checkpoint.lastCreatedAt())
        .build();
  }
}
