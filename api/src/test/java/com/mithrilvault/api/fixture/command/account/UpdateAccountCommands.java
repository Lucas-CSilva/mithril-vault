package com.mithrilvault.api.fixture.command.account;

import com.mithrilvault.api.domain.command.account.UpdateAccountCommand;

public final class UpdateAccountCommands {

  public static final String UPDATED_NAME = "Bradesco Corrente";

  private UpdateAccountCommands() {}

  public static UpdateAccountCommand withName() {
    return new UpdateAccountCommand(UPDATED_NAME, null, null, null);
  }
}
