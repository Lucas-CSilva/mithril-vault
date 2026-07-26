package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.port.TransactionReadRepository;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.infrastructure.mapper.TransactionMapper;
import com.mithrilvault.api.infrastructure.persistence.TransactionMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryAdapter
    implements TransactionRepository, TransactionReadRepository {

  private final TransactionMapper transactionMapper;
  private final TransactionMongoRepository transactionMongoRepository;

  @Override
  public Mono<Transaction> findByIdAndOwnerId(String id, String ownerId) {
    return transactionMongoRepository
        .findByIdAndOwnerId(id, ownerId)
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
}
