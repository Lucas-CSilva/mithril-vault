package com.mithrilvault.api.domain.service;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.service.validation.TransactionValidationRule;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionValidationService {

  private final List<TransactionValidationRule> rules;

  public Mono<Void> validate(CreateTransactionCommand command) {
    return Flux.fromIterable(rules)
        .filter(rule -> rule.appliesTo(command))
        .concatMap(rule -> rule.validate(command))
        .then();
  }
}
