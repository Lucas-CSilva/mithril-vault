package com.mithrilvault.api.infrastructure.adapter.messaging;

import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import lombok.Builder;

@Builder
public record BalanceProjectionMessage(
    String ownerId,
    String transactionId,
    String accountId,
    String invoiceId,
    TransactionType type,
    Long amount,
    ProjectionTarget target) {

  public static BalanceProjectionMessage of(TransactionDocument transaction) {
    return BalanceProjectionMessage.builder()
        .ownerId(transaction.getOwnerId())
        .transactionId(transaction.getId())
        .accountId(transaction.getAccountId())
        .invoiceId(transaction.getInvoiceId())
        .type(transaction.getType())
        .amount(transaction.getAmount())
        .target(
            transaction.getInvoiceId() != null
                ? ProjectionTarget.INVOICE
                : ProjectionTarget.ACCOUNT)
        .build();
  }
}
