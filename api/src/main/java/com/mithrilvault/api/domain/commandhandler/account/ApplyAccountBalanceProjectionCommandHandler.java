package com.mithrilvault.api.domain.commandhandler.account;

import com.mithrilvault.api.domain.command.account.ApplyAccountBalanceProjectionCommand;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.port.ProjectionRepository;
import com.mithrilvault.api.domain.port.TransactionReadRepository;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyAccountBalanceProjectionCommandHandler {
  private final ProjectionRepository projectionRepository;
  private final TransactionReadRepository transactionReadRepository;

  public Mono<Void> handle(ApplyAccountBalanceProjectionCommand command) {
    return transactionReadRepository
        .findByIdAndOwnerId(command.transactionId(), command.ownerId())
        .filter(transaction -> isNotYetApplied(transaction, command))
        .flatMap(transaction -> applyProjection(command, transaction))
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "Skipping balance projection {} for transaction {}: transaction not found for owner, or"
                            + " projection already applied (duplicate delivery)",
                        command.projectionId(),
                        command.transactionId())));
  }

  private boolean isNotYetApplied(
      Transaction transaction, ApplyAccountBalanceProjectionCommand command) {
    return !transaction.appliedProjections().contains(command.projectionId());
  }

  private Mono<Void> applyProjection(
      ApplyAccountBalanceProjectionCommand command, Transaction transaction) {
    var appliedProjections = new HashSet<>(transaction.appliedProjections());
    appliedProjections.add(command.projectionId());
    var updatedTransaction = transaction.toBuilder().appliedProjections(appliedProjections).build();

    long signedAmount = command.type().signedAmount(command.amount());

    return projectionRepository
        .markAppliedAndUpdateBalance(
            command.projectionId(),
            command.ownerId(),
            command.accountId(),
            signedAmount,
            updatedTransaction)
        .doOnSuccess(
            v ->
                log.info(
                    "Applied balance projection {} for transaction {} on account {}",
                    command.projectionId(),
                    command.transactionId(),
                    command.accountId()));
  }
}
