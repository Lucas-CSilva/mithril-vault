package com.mithrilvault.api.fixture.command.account;

import com.mithrilvault.api.domain.command.account.ReconcileAccountCommand;
import com.mithrilvault.api.domain.model.ReconciliationMethod;

public final class ReconcileAccountCommands {

  public static final Long DEFAULT_REAL_BALANCE = 148500L;

  private ReconcileAccountCommands() {}

  public static ReconcileAccountCommand adjustInitialBalance() {
    return new ReconcileAccountCommand(
        DEFAULT_REAL_BALANCE, ReconciliationMethod.ADJUST_INITIAL_BALANCE);
  }

  public static ReconcileAccountCommand adjustingTransaction() {
    return new ReconcileAccountCommand(
        DEFAULT_REAL_BALANCE, ReconciliationMethod.ADJUSTING_TRANSACTION);
  }
}
