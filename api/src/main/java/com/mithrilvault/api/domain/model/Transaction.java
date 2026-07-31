package com.mithrilvault.api.domain.model;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import lombok.Builder;

@Builder(toBuilder = true)
public record Transaction(
    String id,
    String ownerId,
    TransactionType type,
    Long amount,
    LocalDate date,
    String description,
    String categoryId,
    PaymentMethod paymentMethod,
    String accountId,
    String invoiceId,
    AccountSummary account,
    CardSummary card,
    InvoiceSummary invoice,
    Set<String> tags,
    String notes,
    Boolean isRecurring,
    String recurringSeriesId,
    Installment installment,
    String transferPairId,
    String importHash,
    String fitid,
    ImportSource importSource,
    Boolean isReconciliation,
    Set<String> appliedProjections,
    Instant createdAt,
    Instant updatedAt,
    Long version) {

  public static Transaction accountTransaction(
      CreateTransactionCommand command, Account account, String ownerId) {
    return Transaction.builder()
        .ownerId(ownerId)
        .type(command.type())
        .amount(command.amount())
        .date(command.date())
        .description(command.description())
        .categoryId(command.categoryId())
        .paymentMethod(command.paymentMethod())
        .accountId(account.id())
        .account(new AccountSummary(account.id(), account.name()))
        .tags(command.tags())
        .notes(command.notes())
        .appliedProjections(Set.of())
        .build();
  }

  public static Transaction debitCardTransaction(
      CreateTransactionCommand command, Card card, String ownerId) {
    return Transaction.builder()
        .ownerId(ownerId)
        .type(command.type())
        .amount(command.amount())
        .date(command.date())
        .description(command.description())
        .categoryId(command.categoryId())
        .paymentMethod(command.paymentMethod())
        .accountId(card.accountId())
        .card(new CardSummary(card.id(), card.name()))
        .tags(command.tags())
        .notes(command.notes())
        .appliedProjections(Set.of())
        .build();
  }

  public static Transaction creditCardTransaction(
      CreateTransactionCommand command, Card card, Invoice invoice, String ownerId) {
    return Transaction.builder()
        .ownerId(ownerId)
        .type(command.type())
        .amount(command.amount())
        .date(command.date())
        .description(command.description())
        .categoryId(command.categoryId())
        .paymentMethod(command.paymentMethod())
        .invoiceId(invoice.id())
        .card(new CardSummary(card.id(), card.name()))
        .invoice(new InvoiceSummary(invoice.id()))
        .tags(command.tags())
        .notes(command.notes())
        .appliedProjections(Set.of())
        .build();
  }
}
