package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.BalanceSnapshot;
import reactor.core.publisher.Mono;

public interface BalanceSnapshotRepository {
  Mono<BalanceSnapshot> save(BalanceSnapshot snapshot);
}
