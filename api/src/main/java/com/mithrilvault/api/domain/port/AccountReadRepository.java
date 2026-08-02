package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.BalancePoint;
import reactor.core.publisher.Flux;

public interface AccountReadRepository {
  Flux<BalancePoint> balanceHistory(
      String accountId, String ownerId, Long currentBalance, int days);
}
