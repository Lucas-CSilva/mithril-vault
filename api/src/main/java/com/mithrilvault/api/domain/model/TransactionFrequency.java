package com.mithrilvault.api.domain.model;

import java.time.LocalDate;
import java.util.function.UnaryOperator;

public enum TransactionFrequency {
  WEEKLY(date -> date.plusWeeks(1)),
  BIWEEKLY(date -> date.plusWeeks(2)),
  MONTHLY(date -> date.plusMonths(1)),
  BIMONTHLY(date -> date.plusMonths(2)),
  QUARTERLY(date -> date.plusMonths(3)),
  SEMIANNUAL(date -> date.plusMonths(6)),
  ANNUAL(date -> date.plusYears(1));

  private final UnaryOperator<LocalDate> advancer;

  TransactionFrequency(UnaryOperator<LocalDate> advancer) {
    this.advancer = advancer;
  }

  public LocalDate advance(LocalDate date) {
    return advancer.apply(date);
  }
}
