package com.mithrilvault.api.domain.commandhandler.account;

import com.mithrilvault.api.domain.command.account.UpdateAccountCommand;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateAccountCommandHandler {
  private final AccountRepository accountRepository;

  public Mono<Account> handle(String id, String ownerId, UpdateAccountCommand command) {
    log.info("Updating account id={} ownerId={}", id, ownerId);
    return accountRepository
        .findByIdAndOwnerId(id, ownerId)
        .switchIfEmpty(Mono.error(new NotFoundException("Account not found")))
        .map(account -> account.update(command))
        .flatMap(accountRepository::save)
        .doOnSuccess(
            account -> log.info("Account updated id={} ownerId={}", account.id(), ownerId));
  }
}
