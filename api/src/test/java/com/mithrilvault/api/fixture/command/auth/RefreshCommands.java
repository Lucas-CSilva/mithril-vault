package com.mithrilvault.api.fixture.command.auth;

import com.mithrilvault.api.domain.command.auth.RefreshCommand;

public final class RefreshCommands {

  public static final String DEFAULT_RAW_TOKEN = "raw-refresh-token-abc";

  private RefreshCommands() {}

  public static RefreshCommand valid() {
    return new RefreshCommand(DEFAULT_RAW_TOKEN);
  }

  public static RefreshCommand withToken(String rawToken) {
    return new RefreshCommand(rawToken);
  }
}
