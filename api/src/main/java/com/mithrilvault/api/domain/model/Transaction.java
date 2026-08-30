package com.mithrilvault.api.domain.model;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

  public static Transaction fromCommand(
      CreateTransactionCommand command, TransactionContext context) {
    TransactionBuilder builder = buildFromCommand(command, context.ownerId());
    applyOrigin(builder, context.origin());

    if (context.recurringSeriesId() != null) {
      builder.isRecurring(true).recurringSeriesId(context.recurringSeriesId());
    }

    return builder.build();
  }

  private static void applyOrigin(TransactionBuilder builder, TransactionOrigin origin) {
    if (origin.invoice() != null) {
      builder
          .invoiceId(origin.invoice().id())
          .card(new CardSummary(origin.card().id(), origin.card().name()))
          .invoice(new InvoiceSummary(origin.invoice().id()));
      return;
    }

    if (origin.card() != null) {
      builder
          .accountId(origin.card().accountId())
          .card(new CardSummary(origin.card().id(), origin.card().name()));
      return;
    }

    builder
        .accountId(origin.account().id())
        .account(new AccountSummary(origin.account().id(), origin.account().name()));
  }

  public static List<Transaction> transferLeg(
      CreateTransactionCommand command,
      Account sourceAccount,
      Account targetAccount,
      String ownerId,
      String transferPairId) {

    var source =
        buildFromCommand(command, ownerId)
            .accountId(sourceAccount.id())
            .account(new AccountSummary(sourceAccount.id(), sourceAccount.name()))
            .transferPairId(transferPairId)
            .paymentMethod(PaymentMethod.TRANSFER)
            .type(TransactionType.DEBIT)
            .build();

    var target =
        buildFromCommand(command, ownerId)
            .accountId(targetAccount.id())
            .account(new AccountSummary(targetAccount.id(), targetAccount.name()))
            .transferPairId(transferPairId)
            .paymentMethod(PaymentMethod.TRANSFER)
            .type(TransactionType.CREDIT)
            .build();

    return List.of(source, target);
  }

  private static TransactionBuilder buildFromCommand(
      CreateTransactionCommand command, String ownerId) {
    return Transaction.builder()
        .ownerId(ownerId)
        .type(command.type())
        .amount(command.amount())
        .date(command.date())
        .description(command.description())
        .categoryId(command.categoryId())
        .paymentMethod(command.paymentMethod())
        .tags(command.tags())
        .notes(command.notes())
        .appliedProjections(Set.of());
  }
}
