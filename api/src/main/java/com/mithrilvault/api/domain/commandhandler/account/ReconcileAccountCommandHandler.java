package com.mithrilvault.api.domain.commandhandler.account;

import com.mithrilvault.api.domain.command.account.ReconcileAccountCommand;
import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.exception.ErrorCode;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.ReconciliationMethod;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconcileAccountCommandHandler {

  private final AccountRepository accountRepository;
  private final AccountReadRepository accountReadRepository;

  public Mono<Account> handle(String id, String ownerId, ReconcileAccountCommand command) {
    log.info("Reconciling account id={} ownerId={} method={}", id, ownerId, command.method());
    return accountRepository
        .findByIdAndOwnerId(id, ownerId)
        .switchIfEmpty(Mono.error(new NotFoundException("Account not found")))
        .flatMap(account -> applyReconciliation(account, command))
        .flatMap(accountRepository::save)
        .doOnSuccess(account -> log.info("Account reconciled id={} ownerId={}", id, ownerId));
  }

  private Mono<Account> applyReconciliation(Account account, ReconcileAccountCommand command) {
    if (command.method() == ReconciliationMethod.ADJUSTING_TRANSACTION) {
      return Mono.error(
          new BusinessException(
              ErrorCode.VALIDATION_FAILED,
              "Reconciliation via ADJUSTING_TRANSACTION is not supported yet"));
    }

    return accountReadRepository
        .currentBalance(account.id(), account.ownerId(), account.initialBalance())
        .map(
            currentBalance ->
                account.reconcileBalances(
                    account.initialBalance() + (command.realBalance() - currentBalance),
                    command.realBalance()));
  }
}
