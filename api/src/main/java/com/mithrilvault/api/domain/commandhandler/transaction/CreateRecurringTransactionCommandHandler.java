package com.mithrilvault.api.domain.commandhandler.transaction;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionContext;
import com.mithrilvault.api.domain.port.RecurringSeriesRepository;
import com.mithrilvault.api.domain.service.TransactionOriginResolver;
import com.mithrilvault.api.domain.service.TransactionValidationService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateRecurringTransactionCommandHandler {

  private final Clock clock;
  private final TransactionValidationService validationService;
  private final TransactionOriginResolver originResolver;
  private final RecurringSeriesRepository recurringSeriesRepository;

  public Flux<Transaction> handle(CreateTransactionCommand command, String ownerId) {
    String recurringSeriesId = UUID.randomUUID().toString();

    return validationService
        .validate(command)
        .then(Mono.defer(() -> originResolver.resolve(command, ownerId)))
        .map(origin -> new TransactionContext(ownerId, origin, recurringSeriesId))
        .flatMapMany(
            context ->
                isDue(command)
                    ? handleDueInstance(command, context)
                    : handleFutureSeries(command, context));
  }

  private boolean isDue(CreateTransactionCommand command) {
    return !command.date().isAfter(LocalDate.now(clock));
  }

  private Flux<Transaction> handleFutureSeries(
      CreateTransactionCommand command, TransactionContext context) {
    return Mono.just(RecurringTransactionSeries.fromCommand(command, context, command.date()))
        .flatMap(recurringSeriesRepository::save)
        .thenMany(Flux.empty());
  }

  private Flux<Transaction> handleDueInstance(
      CreateTransactionCommand command, TransactionContext context) {
    Transaction transaction = Transaction.fromCommand(command, context);
    RecurringTransactionSeries series =
        RecurringTransactionSeries.fromCommand(
            command, context, command.recurring().frequency().advance(command.date()));

    return recurringSeriesRepository.saveWithInstance(series, transaction);
  }
}
