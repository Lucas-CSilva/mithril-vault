package com.mithrilvault.api.domain.model;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import lombok.Builder;

@Builder
public record RecurringTransactionSeries(
    String id,
    String ownerId,
    String recurringSeriesId,
    TransactionFrequency frequency,
    LocalDate endDate,
    LocalDate nextOccurrenceDate,
    TransactionType type,
    Long amount,
    String description,
    String categoryId,
    PaymentMethod paymentMethod,
    String accountId,
    Set<String> tags,
    String notes,
    Instant createdAt,
    Long version) {

  public static RecurringTransactionSeries fromCommand(
      CreateTransactionCommand command, TransactionContext context, LocalDate nextOccurrenceDate) {
    return RecurringTransactionSeries.builder()
        .ownerId(context.ownerId())
        .recurringSeriesId(context.recurringSeriesId())
        .frequency(command.recurring().frequency())
        .endDate(command.recurring().endDate())
        .nextOccurrenceDate(nextOccurrenceDate)
        .type(command.type())
        .amount(command.amount())
        .description(command.description())
        .categoryId(command.categoryId())
        .paymentMethod(command.paymentMethod())
        .accountId(context.origin().account().id())
        .tags(command.tags())
        .notes(command.notes())
        .build();
  }

  public CreateTransactionCommand toCommand() {
    return CreateTransactionCommand.builder()
        .mode(TransactionMode.SINGLE)
        .type(this.type)
        .amount(this.amount)
        .date(this.nextOccurrenceDate)
        .description(this.description)
        .categoryId(this.categoryId)
        .paymentMethod(this.paymentMethod)
        .accountId(this.accountId)
        .tags(this.tags)
        .notes(this.notes)
        .build();
  }
}
