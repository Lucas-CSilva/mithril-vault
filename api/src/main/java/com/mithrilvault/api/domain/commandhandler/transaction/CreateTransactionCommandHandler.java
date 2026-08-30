package com.mithrilvault.api.domain.commandhandler.transaction;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionContext;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.domain.service.TransactionOriginResolver;
import com.mithrilvault.api.domain.service.TransactionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTransactionCommandHandler {

  private final TransactionRepository transactionRepository;
  private final TransactionOriginResolver originResolver;
  private final TransactionValidationService validationService;

  public Flux<Transaction> handle(CreateTransactionCommand command, String ownerId) {
    return handle(command, ownerId, null);
  }

  public Flux<Transaction> handle(
      CreateTransactionCommand command, String ownerId, String recurringSeriesId) {

    return validationService
        .validate(command)
        .then(Mono.defer(() -> originResolver.resolve(command, ownerId)))
        .map(
            origin ->
                Transaction.fromCommand(
                    command, new TransactionContext(ownerId, origin, recurringSeriesId)))
        .flatMap(transactionRepository::save)
        .flux();
  }
}
