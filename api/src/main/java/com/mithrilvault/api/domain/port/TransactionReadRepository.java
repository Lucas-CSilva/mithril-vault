package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.DailyNetAmount;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionAggregate;
import java.time.Instant;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionReadRepository {
  Mono<Transaction> findByIdAndOwnerId(String id, String ownerId);

  Flux<Transaction> findByTransferPairId(String ownerId, String transferPairId);

  Mono<TransactionAggregate> netAmount(String accountId, String ownerId);

  Mono<TransactionAggregate> netAmountAfter(
      String accountId, String ownerId, String transactionId, Instant createdAt);

  Flux<DailyNetAmount> netAmountByDate(
      String accountId, String ownerId, LocalDate from, LocalDate to);

  Mono<Boolean> existsByTransferPairId(String ownerId, String transferPairId);
}
