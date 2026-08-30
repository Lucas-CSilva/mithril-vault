package com.mithrilvault.api.domain.service.validation;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import reactor.core.publisher.Mono;

public interface TransactionValidationRule {
  boolean appliesTo(CreateTransactionCommand command);

  Mono<Void> validate(CreateTransactionCommand command);
}
