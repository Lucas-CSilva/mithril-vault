package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Transaction;
import reactor.core.publisher.Mono;

public interface ProjectionRepository {

  Mono<Void> markAppliedAndUpdateBalance(
      String projectionId,
      String ownerId,
      String accountId,
      Long signedAmount,
      Transaction transaction);
}
