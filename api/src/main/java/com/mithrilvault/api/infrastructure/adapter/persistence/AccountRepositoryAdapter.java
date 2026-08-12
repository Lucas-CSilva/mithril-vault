package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.*;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.domain.port.TransactionReadRepository;
import com.mithrilvault.api.infrastructure.mapper.AccountMapper;
import com.mithrilvault.api.infrastructure.mapper.BalanceSnapshotMapper;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.BalanceSnapshotDocument;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository, AccountReadRepository {

  private final AccountMapper accountMapper;
  private final BalanceSnapshotMapper snapshotMapper;
  private final AccountMongoRepository accountRepository;
  private final ReactiveMongoTemplate reactiveMongoTemplate;
  private final TransactionReadRepository transactionRepository;

  private static final Sort LATEST_SNAPSHOT_FIRST =
      Sort.by(Sort.Direction.DESC, BalanceSnapshotDocument.Fields.asOfDate);

  @Override
  public Flux<Account> findAllActive() {
    return accountRepository.findAllByIsActiveTrue().map(accountMapper::toDomain);
  }

  @Override
  public Mono<Account> save(Account account) {
    return accountRepository
        .save(accountMapper.toDocument(account))
        .onErrorMap(
            DuplicateKeyException.class, ex -> new ConflictException("Account name already used"))
        .onErrorMap(
            OptimisticLockingFailureException.class,
            ex -> new ConflictException("Account was modified concurrently, please retry"))
        .map(accountMapper::toDomain);
  }

  @Override
  public Mono<Account> findByIdAndOwnerId(String id, String ownerId) {
    return accountRepository.findByIdAndOwnerId(id, ownerId).map(accountMapper::toDomain);
  }

  @Override
  public Flux<Account> findAllByOwnerId(String ownerId, boolean includeInactive) {
    return (includeInactive
            ? accountRepository.findAllByOwnerId(ownerId)
            : accountRepository.findAllByOwnerIdAndIsActiveTrue(ownerId))
        .map(accountMapper::toDomain);
  }

  @Override
  public Flux<BalancePoint> balanceHistory(String accountId, String ownerId, int days) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate windowStart = today.minusDays(days - 1L);

    return recomputeBalance(accountId, ownerId)
        .flatMapMany(
            groundTruthBalance ->
                transactionRepository
                    .netAmountByDate(accountId, ownerId, windowStart, today)
                    .collectMap(DailyNetAmount::date, DailyNetAmount::netAmount)
                    .flatMapMany(
                        netChangeByDate -> {
                          long windowNetChange =
                              netChangeByDate.values().stream().mapToLong(Long::longValue).sum();
                          long seed = groundTruthBalance - windowNetChange;
                          return Flux.range(0, days)
                              .map(windowStart::plusDays)
                              .scan(
                                  new BalancePoint(windowStart.minusDays(1), seed),
                                  (previous, date) ->
                                      new BalancePoint(
                                          date,
                                          previous.balance()
                                              + netChangeByDate.getOrDefault(date, 0L)))
                              .skip(1);
                        }));
  }

  @Override
  public Mono<TransactionAggregate> computeSnapshot(String accountId, String ownerId) {
    return computeCheckpoint(accountId, ownerId);
  }

  @Override
  public Mono<Long> recomputeBalance(String accountId, String ownerId) {
    return computeCheckpoint(accountId, ownerId).map(TransactionAggregate::balance);
  }

  private Criteria snapshotScopeCriteria(String accountId, String ownerId) {
    return Criteria.where(BalanceSnapshotDocument.Fields.accountId)
        .is(accountId)
        .and(BalanceSnapshotDocument.Fields.ownerId)
        .is(ownerId);
  }

  private Mono<BalanceSnapshot> findLatestSnapshot(String accountId, String ownerId) {
    return reactiveMongoTemplate
        .findOne(
            Query.query(snapshotScopeCriteria(accountId, ownerId)).with(LATEST_SNAPSHOT_FIRST),
            BalanceSnapshotDocument.class)
        .map(snapshotMapper::toDomain);
  }

  private Mono<TransactionAggregate> checkpointFromSnapshot(
      String accountId, String ownerId, BalanceSnapshot snapshot) {
    return transactionRepository
        .netAmountAfter(accountId, ownerId, snapshot.lastTransactionId(), snapshot.lastCreatedAt())
        .map(delta -> delta.toBuilder().balance(snapshot.balance() + delta.balance()).build());
  }

  private Mono<TransactionAggregate> checkpointFromInitialBalance(
      String accountId, String ownerId) {
    return findByIdAndOwnerId(accountId, ownerId)
        .flatMap(
            account ->
                transactionRepository
                    .netAmount(accountId, ownerId)
                    .map(
                        delta ->
                            delta.toBuilder()
                                .balance(account.initialBalance() + delta.balance())
                                .build()));
  }

  private Mono<TransactionAggregate> computeCheckpoint(String accountId, String ownerId) {
    return findLatestSnapshot(accountId, ownerId)
        .flatMap(snapshot -> checkpointFromSnapshot(accountId, ownerId, snapshot))
        .switchIfEmpty(checkpointFromInitialBalance(accountId, ownerId));
  }
}
