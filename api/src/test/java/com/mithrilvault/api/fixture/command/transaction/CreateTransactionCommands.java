package com.mithrilvault.api.fixture.command.transaction;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.model.TransactionMode;
import com.mithrilvault.api.domain.model.TransactionType;
import java.time.LocalDate;
import java.util.Set;

public final class CreateTransactionCommands {

  public static final Long DEFAULT_AMOUNT = 15090L;
  public static final String DEFAULT_DESCRIPTION = "Supermercado";

  private CreateTransactionCommands() {}

  public static CreateTransactionCommand validForAccount(String accountId) {
    return CreateTransactionCommand.builder()
        .mode(TransactionMode.SINGLE)
        .type(TransactionType.DEBIT)
        .amount(DEFAULT_AMOUNT)
        .date(LocalDate.of(2026, 7, 20))
        .description(DEFAULT_DESCRIPTION)
        .paymentMethod(PaymentMethod.PIX)
        .accountId(accountId)
        .tags(Set.of())
        .build();
  }

  public static CreateTransactionCommand withBothAccountAndCard(String accountId, String cardId) {
    return CreateTransactionCommand.builder()
        .mode(TransactionMode.SINGLE)
        .type(TransactionType.DEBIT)
        .amount(DEFAULT_AMOUNT)
        .date(LocalDate.of(2026, 7, 20))
        .description(DEFAULT_DESCRIPTION)
        .paymentMethod(PaymentMethod.PIX)
        .accountId(accountId)
        .cardId(cardId)
        .tags(Set.of())
        .build();
  }

  public static CreateTransactionCommand withPaymentMethod(
      String accountId, PaymentMethod paymentMethod) {
    return CreateTransactionCommand.builder()
        .mode(TransactionMode.SINGLE)
        .type(TransactionType.DEBIT)
        .amount(DEFAULT_AMOUNT)
        .date(LocalDate.of(2026, 7, 20))
        .description(DEFAULT_DESCRIPTION)
        .paymentMethod(paymentMethod)
        .accountId(accountId)
        .tags(Set.of())
        .build();
  }
}
