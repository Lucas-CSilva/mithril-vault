package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalancePoint;
import com.mithrilvault.api.domain.model.TransactionAggregate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountReadRepository {
  Flux<BalancePoint> balanceHistory(
      String accountId, String ownerId, Long currentBalance, int days);

  Mono<TransactionAggregate> computeSnapshot(String accountId, String ownerId);

  Mono<Long> recomputeBalance(String accountId, String ownerId);

  Flux<Account> findAllActive();
}
