package com.mithrilvault.api.domain.commandhandler.transaction;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.exception.NotImplementedException;
import com.mithrilvault.api.domain.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTransferCommandHandler {

  // TODO(004-transactions): implement TRANSFER mode (two linked legs via TransactionalOperator,
  // idempotent on transferPairId — see implementation-notes.md §5).
  public Mono<Transaction> handle(CreateTransactionCommand command, String ownerId) {
    return Mono.error(
        new NotImplementedException("TRANSFER transaction mode is not implemented yet"));
  }
}
