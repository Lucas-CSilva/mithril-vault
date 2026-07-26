package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface TransactionMongoRepository
    extends ReactiveMongoRepository<TransactionDocument, String> {

  Mono<TransactionDocument> findByIdAndOwnerId(String id, String ownerId);
}
