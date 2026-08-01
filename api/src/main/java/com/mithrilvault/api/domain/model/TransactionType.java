package com.mithrilvault.api.domain.model;

public enum TransactionType {
  DEBIT,
  CREDIT;

  public long signedAmount(long amount) {
    return this == DEBIT ? -amount : amount;
  }
}
