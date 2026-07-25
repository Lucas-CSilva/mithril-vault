package com.mithrilvault.api.domain.commandhandler.account;

import com.mithrilvault.api.domain.command.account.CreateAccountCommand;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateAccountCommandHandler {

  private final AccountRepository accountRepository;

  public Mono<Account> handle(String ownerId, CreateAccountCommand command) {
    log.info(
        "Creating account ownerId={} name={} type={}", ownerId, command.name(), command.type());
    return accountRepository
        .save(Account.create(ownerId, command))
        .doOnSuccess(
            account -> log.info("Account created id={} ownerId={}", account.id(), ownerId));
  }
}
