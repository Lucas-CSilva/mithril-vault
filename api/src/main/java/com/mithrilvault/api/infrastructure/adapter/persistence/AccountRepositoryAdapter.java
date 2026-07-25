package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalancePoint;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.infrastructure.mapper.AccountMapper;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository, AccountReadRepository {

  private static final String TRANSACTIONS_COLLECTION = "transactions";

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
  public Mono<Long> currentBalance(String accountId, String ownerId, Long initialBalance) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("ownerId").is(ownerId).and("accountId").is(accountId)),
            Aggregation.group("type").sum("amount").as("total"));

    return reactiveMongoTemplate
        .aggregate(aggregation, TRANSACTIONS_COLLECTION, TransactionTypeTotal.class)
        .collectList()
        .map(
            totals ->
                initialBalance
                    + totals.stream()
                        .mapToLong(
                            total -> "CREDIT".equals(total.id()) ? total.total() : -total.total())
                        .sum());
  }

  @Override
  public Flux<BalancePoint> balanceHistory(
      String accountId, String ownerId, Long initialBalance, int days) {
    // The transactions collection (feature 004) doesn't exist yet, so every day's closing
    // balance is the account's initialBalance until real transactions can be aggregated.
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    return Flux.range(0, days)
        .map(offset -> new BalancePoint(today.minusDays(days - 1L - offset), initialBalance));
  }

  private record TransactionTypeTotal(String id, Long total) {}
}
