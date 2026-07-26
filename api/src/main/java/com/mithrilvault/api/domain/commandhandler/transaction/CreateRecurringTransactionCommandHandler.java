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
public class CreateRecurringTransactionCommandHandler {

  // TODO(004-transactions): implement RECURRING mode (series generation by frequency/end date,
  // recurringSeriesId linkage — see data-model.md).
  public Mono<Transaction> handle(CreateTransactionCommand command, String ownerId) {
    return Mono.error(
        new NotImplementedException("RECURRING transaction mode is not implemented yet"));
  }
}
