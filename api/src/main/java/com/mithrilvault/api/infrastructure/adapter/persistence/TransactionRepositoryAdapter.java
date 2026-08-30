package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.DailyNetAmount;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionAggregate;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.domain.port.TransactionReadRepository;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.infrastructure.mapper.TransactionMapper;
import com.mithrilvault.api.infrastructure.persistence.TransactionMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.BaseDocument;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryAdapter
    implements TransactionRepository, TransactionReadRepository {

  private final TransactionMapper transactionMapper;
  private final ReactiveMongoTemplate mongoTemplate;
  private final TransactionalOperator transactionalOperator;
  private final TransactionMongoRepository transactionMongoRepository;

  private static final String NET_AMOUNT_ALIAS = "netAmount";

  private static final AggregationExpression SIGNED_AMOUNT =
      ConditionalOperators.when(
              Criteria.where(TransactionDocument.Fields.type).is(TransactionType.DEBIT))
          .then(
              ArithmeticOperators.Multiply.valueOf(TransactionDocument.Fields.amount)
                  .multiplyBy(-1))
          .otherwise("$" + TransactionDocument.Fields.amount);

  private static final GroupOperation SCOPED_TRANSACTIONS =
      Aggregation.group().sum(SIGNED_AMOUNT).as(NET_AMOUNT_ALIAS);

  private record NetAmountResult(Long netAmount) {}

  private record Cursor(String transactionId, Instant createdAt) {
    public static Cursor empty() {
      return new Cursor(null, null);
    }
  }

  private static TransactionAggregate toTransactionAggregate(Long amount, Cursor cursor) {
    return new TransactionAggregate(amount, cursor.transactionId(), cursor.createdAt());
  }

  @Override
  public Mono<Transaction> findByIdAndOwnerId(String id, String ownerId) {
    return transactionMongoRepository
        .findByIdAndOwnerId(id, ownerId)
        .map(transactionMapper::toDomain);
  }

  @Override
  public Flux<Transaction> findByTransferPairId(String ownerId, String transferPairId) {
    return transactionMongoRepository
        .findByTransferPairIdAndOwnerId(transferPairId, ownerId)
        .map(transactionMapper::toDomain);
  }

  @Override
  public Mono<Transaction> save(Transaction transaction) {
    return transactionMongoRepository
        .save(transactionMapper.toDocument(transaction))
        .onErrorMap(
            DuplicateKeyException.class,
            ex -> new ConflictException("Transaction already imported (duplicate)"))
        .onErrorMap(
            OptimisticLockingFailureException.class,
            ex -> new ConflictException("Transaction was modified concurrently, please retry"))
        .map(transactionMapper::toDomain);
  }

  @Override
  public Flux<Transaction> saveAll(List<Transaction> transactions) {

    return transactionalOperator.execute(
        tx ->
            transactionMongoRepository
                .saveAll(transactions.stream().map(transactionMapper::toDocument).toList())
                .map(transactionMapper::toDomain));
  }

  private Criteria accountScopeCriteria(String ownerId, String accountId) {
    return Criteria.where(TransactionDocument.Fields.ownerId)
        .is(ownerId)
        .and(TransactionDocument.Fields.accountId)
        .is(accountId);
  }

  private Criteria cursorCriteria(String transactionId, Instant lastCreatedAt) {
    return new Criteria()
        .orOperator(
            Criteria.where(BaseDocument.Fields.createdAt).gt(lastCreatedAt),
            new Criteria()
                .andOperator(
                    Criteria.where(BaseDocument.Fields.createdAt).is(lastCreatedAt),
                    Criteria.where("_id").gt(transactionId)));
  }

  private Mono<Long> sumNetAmount(Criteria scope) {
    return mongoTemplate
        .aggregate(
            Aggregation.newAggregation(Aggregation.match(scope), SCOPED_TRANSACTIONS),
            TransactionDocument.class,
            NetAmountResult.class)
        .next()
        .map(NetAmountResult::netAmount)
        .defaultIfEmpty(0L);
  }

  private Mono<Cursor> resolveCursor(Criteria scope, Cursor defaultCursor) {
    return mongoTemplate
        .findOne(
            Query.query(scope)
                .with(Sort.by(Sort.Direction.DESC, BaseDocument.Fields.createdAt, "_id")),
            TransactionDocument.class)
        .map(doc -> new Cursor(doc.getId(), doc.getCreatedAt()))
        .defaultIfEmpty(defaultCursor);
  }

  @Override
  public Mono<TransactionAggregate> netAmount(String accountId, String ownerId) {

    Criteria scope = accountScopeCriteria(ownerId, accountId);

    return Mono.zip(
        sumNetAmount(scope),
        resolveCursor(scope, Cursor.empty()),
        TransactionRepositoryAdapter::toTransactionAggregate);
  }

  @Override
  public Mono<TransactionAggregate> netAmountAfter(
      String accountId, String ownerId, String transactionId, Instant createdAt) {

    Criteria scope =
        accountScopeCriteria(ownerId, accountId)
            .andOperator(cursorCriteria(transactionId, createdAt));

    return Mono.zip(
        sumNetAmount(scope),
        resolveCursor(scope, new Cursor(transactionId, createdAt)),
        TransactionRepositoryAdapter::toTransactionAggregate);
  }

  @Override
  public Flux<DailyNetAmount> netAmountByDate(
      String accountId, String ownerId, LocalDate from, LocalDate to) {
    return mongoTemplate.aggregate(
        Aggregation.newAggregation(
            Aggregation.match(
                accountScopeCriteria(ownerId, accountId)
                    .and(TransactionDocument.Fields.date)
                    .gte(from)
                    .lte(to)),
            Aggregation.group(TransactionDocument.Fields.date)
                .sum(SIGNED_AMOUNT)
                .as(NET_AMOUNT_ALIAS),
            Aggregation.project().and("_id").as("date").andInclude(NET_AMOUNT_ALIAS)),
        TransactionDocument.class,
        DailyNetAmount.class);
  }

  @Override
  public Mono<Boolean> existsByTransferPairId(String ownerId, String transferPairId) {
    return mongoTemplate.exists(
        Query.query(
            Criteria.where(TransactionDocument.Fields.ownerId)
                .is(ownerId)
                .and(TransactionDocument.Fields.transferPairId)
                .is(transferPairId)),
        TransactionDocument.class);
  }
}
