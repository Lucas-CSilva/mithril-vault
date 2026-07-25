package com.mithrilvault.api.fixture.command.account;

import com.mithrilvault.api.domain.command.account.CreateAccountCommand;
import com.mithrilvault.api.domain.model.AccountType;

public final class CreateAccountCommands {

  public static final String DEFAULT_NAME = "Nubank";
  public static final Long DEFAULT_INITIAL_BALANCE = 150090L;
  public static final String DEFAULT_COLOR = "#88C0D0";

  private CreateAccountCommands() {}

  public static CreateAccountCommand valid() {
    return new CreateAccountCommand(
        DEFAULT_NAME, AccountType.DIGITAL, "Nubank", DEFAULT_INITIAL_BALANCE, DEFAULT_COLOR);
  }

  public static CreateAccountCommand withName(String name) {
    return new CreateAccountCommand(
        name, AccountType.DIGITAL, "Nubank", DEFAULT_INITIAL_BALANCE, DEFAULT_COLOR);
  }
}
