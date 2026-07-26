package com.mithrilvault.api.domain.commandhandler.transaction;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionOrigin;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.domain.service.TransactionOriginResolver;
import com.mithrilvault.api.domain.service.TransactionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTransactionCommandHandler {

  private final TransactionRepository transactionRepository;
  private final TransactionOriginResolver originResolver;
  private final TransactionValidationService validationService;

  public Mono<Transaction> handle(CreateTransactionCommand command, String ownerId) {

    return validationService
        .validate(command)
        .then(Mono.defer(() -> originResolver.resolve(command, ownerId)))
        .map(origin -> buildTransaction(command, origin, ownerId))
        .flatMap(transactionRepository::save);
  }

  private Transaction buildTransaction(
      CreateTransactionCommand command, TransactionOrigin origin, String ownerId) {
    if (origin.invoice() != null) {
      return Transaction.creditCardTransaction(command, origin.card(), origin.invoice(), ownerId);
    }

    if (origin.card() != null) {
      return Transaction.debitCardTransaction(command, origin.card(), ownerId);
    }

    return Transaction.accountTransaction(command, origin.account(), ownerId);
  }
}
