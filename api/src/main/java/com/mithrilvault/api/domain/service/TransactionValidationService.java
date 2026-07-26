package com.mithrilvault.api.domain.service;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
public class TransactionValidationService {
  public Mono<Void> validate(CreateTransactionCommand command) {
    if (StringUtils.hasText(command.accountId()) && StringUtils.hasText(command.cardId())) {

      return Mono.error(
          new BusinessException(
              ErrorCode.VALIDATION_FAILED, "Exactly one of accountId or cardId must be set"));
    }

    return Mono.empty();
  }
}
