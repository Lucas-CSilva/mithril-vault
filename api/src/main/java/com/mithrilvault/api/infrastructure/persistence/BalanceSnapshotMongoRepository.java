package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.BalanceSnapshotDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface BalanceSnapshotMongoRepository
    extends ReactiveMongoRepository<BalanceSnapshotDocument, String> {}
