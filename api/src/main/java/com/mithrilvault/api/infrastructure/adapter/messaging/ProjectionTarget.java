package com.mithrilvault.api.infrastructure.adapter.messaging;

public enum ProjectionTarget {
  ACCOUNT("accountBalance"),
  INVOICE("invoiceTotal");

  private final String projectionName;

  ProjectionTarget(String projectionName) {
    this.projectionName = projectionName;
  }

  /**
   * Stable, deterministic identifier for this projection type — used as the idempotency marker
   * recorded in {@code Transaction.appliedProjections}. Must stay constant across every message
   * about a given transaction (SQS redelivery, change-stream replay after a checkpoint loss, etc.)
   * — never a per-message random value, since the guard relies on seeing the same marker twice to
   * recognize a duplicate.
   */
  public String projectionName() {
    return projectionName;
  }
}
