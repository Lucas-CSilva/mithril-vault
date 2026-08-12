package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.model.BalanceSnapshot;
import com.mithrilvault.api.domain.port.BalanceSnapshotRepository;
import com.mithrilvault.api.infrastructure.mapper.BalanceSnapshotMapper;
import com.mithrilvault.api.infrastructure.persistence.BalanceSnapshotMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class BalanceSnapshotRepositoryAdapter implements BalanceSnapshotRepository {

  private final BalanceSnapshotMapper mapper;
  private final BalanceSnapshotMongoRepository mongoRepository;

  @Override
  public Mono<BalanceSnapshot> save(BalanceSnapshot balanceSnapshot) {
    return mongoRepository.save(mapper.toDocument(balanceSnapshot)).map(mapper::toDomain);
  }
}
