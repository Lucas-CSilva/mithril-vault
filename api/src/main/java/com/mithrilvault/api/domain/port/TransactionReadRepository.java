package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Transaction;
import reactor.core.publisher.Mono;

public interface TransactionReadRepository {
  Mono<Transaction> findByIdAndOwnerId(String id, String ownerId);
}
