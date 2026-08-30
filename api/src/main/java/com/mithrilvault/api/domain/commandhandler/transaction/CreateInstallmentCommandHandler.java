package com.mithrilvault.api.domain.commandhandler.transaction;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.exception.NotImplementedException;
import com.mithrilvault.api.domain.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateInstallmentCommandHandler {

  // TODO(004-transactions): implement INSTALLMENT mode (N transactions split across N invoices,
  // integer-division amount split with remainder-centavo rule — see data-model.md).
  public Flux<Transaction> handle(CreateTransactionCommand command, String ownerId) {
    return Flux.error(
        new NotImplementedException("INSTALLMENT transaction mode is not implemented yet"));
  }
}
