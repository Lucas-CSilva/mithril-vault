package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.port.ProjectionRepository;
import com.mithrilvault.api.infrastructure.mapper.TransactionMapper;
import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import com.mithrilvault.api.infrastructure.persistence.document.BaseDocument;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectionRepositoryAdapter implements ProjectionRepository {

  private static final String PROJECTION_NAME = "accountBalance";

  private final TransactionMapper transactionMapper;
  private final TransactionalOperator transactionalOperator;
  private final ReactiveMongoTemplate reactiveMongoTemplate;
  private final MeterRegistry meterRegistry;

  private Counter appliedCounter;
  private Counter replayNoopCounter;

  @PostConstruct
  public void init() {
    appliedCounter =
        Counter.builder("projection.applied.total")
            .tag("projection", PROJECTION_NAME)
            .description("Balance-projection messages successfully applied")
            .register(meterRegistry);
    replayNoopCounter =
        Counter.builder("projection.consumer.replay.noop.total")
            .tag("projection", PROJECTION_NAME)
            .tag("consumer", "AccountBalanceProjector")
            .description(
                "Redelivered balance-projection messages found already applied at the DB level")
            .register(meterRegistry);
  }

  @Override
  public Mono<Void> markAppliedAndUpdateBalance(
      String projectionId,
      String ownerId,
      String accountId,
      Long signedAmount,
      Transaction transaction) {

    return transactionalOperator
        .execute(
            tx ->
                markApplied(projectionId, transaction)
                    .flatMap(applied -> updateBalance(ownerId, accountId, signedAmount)))
        .then();
  }

  private Mono<TransactionDocument> markApplied(String projectionId, Transaction transaction) {
    var query =
        Query.query(
            Criteria.where("_id")
                .is(transaction.id())
                .and(TransactionDocument.Fields.appliedProjections)
                .ne(projectionId));

    return reactiveMongoTemplate
        .findAndReplace(query, transactionMapper.toDocument(transaction))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  replayNoopCounter.increment();
                  log.info(
                      "Projection {} already applied to transaction {} — no-op (redelivery)",
                      projectionId,
                      transaction.id());
                  return Mono.empty();
                }));
  }

  private Mono<Void> updateBalance(String ownerId, String accountId, Long signedAmount) {
    var query =
        Query.query(
            Criteria.where("_id").is(accountId).and(AccountDocument.Fields.ownerId).is(ownerId));
    var update =
        new Update()
            .inc(AccountDocument.Fields.currentBalance, signedAmount)
            .inc(BaseDocument.Fields.version, 1);

    return reactiveMongoTemplate
        .updateFirst(query, update, AccountDocument.class)
        .filter(result -> result.getMatchedCount() == 1 && result.getModifiedCount() == 1)
        .switchIfEmpty(
            Mono.error(() -> new NotFoundException("Account not found or not owned: " + accountId)))
        .doOnSuccess(
            result -> {
              appliedCounter.increment();
              log.info("Incremented currentBalance for account {}", accountId);
            })
        .doOnError(
            error ->
                log.error("Failed to increment currentBalance for account {}", accountId, error))
        .then();
  }
}
