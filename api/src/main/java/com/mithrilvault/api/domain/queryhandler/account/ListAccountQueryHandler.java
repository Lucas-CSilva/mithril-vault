package com.mithrilvault.api.domain.queryhandler.account;

import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListAccountQueryHandler {
  private final AccountRepository accountRepository;

  public Flux<Account> handle(String ownerId, boolean includeInactive) {
    log.info("Querying accounts ownerId={} includeInactive={}", ownerId, includeInactive);
    return accountRepository.findAllByOwnerId(ownerId, includeInactive);
  }
}
