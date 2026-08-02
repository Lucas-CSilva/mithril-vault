package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalancePoint;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.infrastructure.mapper.AccountMapper;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
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
  private final AccountMongoRepository accountRepository;
  private final ReactiveMongoTemplate reactiveMongoTemplate;

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
  public Flux<BalancePoint> balanceHistory(
      String accountId, String ownerId, Long currentBalance, int days) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate windowStart = today.minusDays(days - 1L);

    // TODO(ADR-003): bound this via balance_snapshots once the reconciliation job exists —
    // an unbounded scan over up to `days` worth of transactions, acceptable at this data volume.
    Query query =
        Query.query(
            Criteria.where(TransactionDocument.Fields.ownerId)
                .is(ownerId)
                .and(TransactionDocument.Fields.accountId)
                .is(accountId)
                .and(TransactionDocument.Fields.date)
                .gte(windowStart)
                .lte(today));

    return reactiveMongoTemplate
        .find(query, TransactionDocument.class)
        .collect(
            Collectors.groupingBy(
                TransactionDocument::getDate,
                Collectors.summingLong(txn -> txn.getType().signedAmount(txn.getAmount()))))
        .flatMapMany(
            netChangeByDate -> {
              long windowNetChange =
                  netChangeByDate.values().stream().mapToLong(Long::longValue).sum();
              long seed = currentBalance - windowNetChange;
              return Flux.range(0, days)
                  .map(windowStart::plusDays)
                  .scan(
                      new BalancePoint(windowStart.minusDays(1), seed),
                      (previous, date) ->
                          new BalancePoint(
                              date, previous.balance() + netChangeByDate.getOrDefault(date, 0L)))
                  .skip(1);
            });
  }
}
