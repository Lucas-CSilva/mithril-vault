package com.mithrilvault.api.domain.queryhandler.account;

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
public class GetAccountQueryHandler {
  private final AccountRepository accountRepository;

  public Mono<Account> handle(String id, String ownerId) {
    log.info("Querying for account id={} ownerId={}", id, ownerId);
    return accountRepository
        .findByIdAndOwnerId(id, ownerId)
        .switchIfEmpty(Mono.error(new NotFoundException("Account not found")))
        .doOnSuccess(account -> log.info("Account found id={} ownerId={}", account.id(), ownerId));
  }
}
