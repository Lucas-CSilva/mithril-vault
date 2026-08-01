package com.mithrilvault.api.fixture.model;

import com.mithrilvault.api.domain.model.AccountSummary;
import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionType;
import java.time.LocalDate;
import java.util.Set;

public final class Transactions {

  private Transactions() {}

  public static Transaction accountTransaction() {
    return Transaction.builder()
        .id("transaction-fixture-1")
        .ownerId(Accounts.DEFAULT_OWNER_ID)
        .type(TransactionType.CREDIT)
        .amount(1_000L)
        .date(LocalDate.parse("2026-01-01"))
        .description("Salary")
        .paymentMethod(PaymentMethod.TRANSFER)
        .accountId(Accounts.checking().id())
        .account(new AccountSummary(Accounts.checking().id(), Accounts.checking().name()))
        .tags(Set.of())
        .appliedProjections(Set.of())
        .build();
  }

  public static Transaction accountTransaction(String id) {
    return accountTransaction().toBuilder().id(id).build();
  }
}
