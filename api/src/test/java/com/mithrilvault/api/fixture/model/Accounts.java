package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.AccountType;
import java.time.Instant;

public final class Accounts {

  public static final String DEFAULT_OWNER_ID = "owner-fixture-1";
  public static final String OTHER_OWNER_ID = "owner-fixture-2";

  private Accounts() {}

  public static Account checking() {
    return Account.builder()
        .id("account-fixture-1")
        .ownerId(DEFAULT_OWNER_ID)
        .name("Nubank")
        .type(AccountType.DIGITAL)
        .institution("Nubank")
        .initialBalance(150090L)
        .color("#88C0D0")
        .isActive(true)
        .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
        .build();
  }

  public static Account checking(String ownerId) {
    return checking().toBuilder().ownerId(ownerId).build();
  }

  public static Account inactive() {
    return checking().toBuilder().isActive(false).build();
  }
}
