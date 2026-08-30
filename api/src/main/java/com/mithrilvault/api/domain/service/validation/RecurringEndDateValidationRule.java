package com.mithrilvault.api.domain.service.validation;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.exception.ErrorCode;
import com.mithrilvault.api.domain.model.TransactionMode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RecurringEndDateValidationRule implements TransactionValidationRule {

  @Override
  public boolean appliesTo(CreateTransactionCommand command) {
    return command.mode() == TransactionMode.RECURRING;
  }

  @Override
  public Mono<Void> validate(CreateTransactionCommand command) {
    if (command.recurring().endDate() != null
        && command.recurring().endDate().isBefore(command.date())) {
      return Mono.error(
          new BusinessException(ErrorCode.VALIDATION_FAILED, "endDate must not be before date"));
    }

    return Mono.empty();
  }
}
