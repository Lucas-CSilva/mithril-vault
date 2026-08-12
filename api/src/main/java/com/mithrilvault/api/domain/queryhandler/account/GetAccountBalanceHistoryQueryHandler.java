package com.mithrilvault.api.domain.queryhandler.account;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.BalancePoint;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetAccountBalanceHistoryQueryHandler {

  private static final int HISTORY_DAYS = 30;

  private final AccountRepository accountRepository;
  private final AccountReadRepository accountReadRepository;

  public Flux<BalancePoint> handle(String accountId, String ownerId) {
    log.info("Querying balance history accountId={} ownerId={}", accountId, ownerId);
    return accountRepository
        .findByIdAndOwnerId(accountId, ownerId)
        .switchIfEmpty(Mono.error(new NotFoundException("Account not found")))
        .thenMany(accountReadRepository.balanceHistory(accountId, ownerId, HISTORY_DAYS));
  }
}
