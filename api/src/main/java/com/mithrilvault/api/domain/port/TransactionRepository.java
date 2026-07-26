package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Transaction;
import reactor.core.publisher.Mono;

public interface TransactionRepository {
  Mono<Transaction> save(Transaction transaction);
}
