package com.mithrilvault.api.domain.service.validation;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Component
public class AccountXorCardValidationRule implements TransactionValidationRule {

  @Override
  public boolean appliesTo(CreateTransactionCommand command) {
    return true;
  }

  @Override
  public Mono<Void> validate(CreateTransactionCommand command) {
    if (StringUtils.hasText(command.accountId()) && StringUtils.hasText(command.cardId())) {
      return Mono.error(
          new BusinessException(
              ErrorCode.VALIDATION_FAILED, "Exactly one of accountId or cardId must be set"));
    }

    return Mono.empty();
  }
}
