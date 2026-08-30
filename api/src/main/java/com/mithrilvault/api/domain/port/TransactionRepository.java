package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Transaction;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionRepository {
  Mono<Transaction> save(Transaction transaction);

  Flux<Transaction> saveAll(List<Transaction> transactions);
}
