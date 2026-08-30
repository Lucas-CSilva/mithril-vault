package com.mithrilvault.api.domain.model;

public record TransactionContext(
    String ownerId, TransactionOrigin origin, String recurringSeriesId) {

  public TransactionContext(String ownerId, TransactionOrigin origin) {
    this(ownerId, origin, null);
  }
}
