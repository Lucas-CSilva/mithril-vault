package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.BalancePoint;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountReadRepository {

  Mono<Long> currentBalance(String accountId, String ownerId, Long initialBalance);

  Flux<BalancePoint> balanceHistory(
      String accountId, String ownerId, Long initialBalance, int days);
}
