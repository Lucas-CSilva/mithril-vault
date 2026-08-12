package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountMongoRepository extends ReactiveMongoRepository<AccountDocument, String> {
  Mono<AccountDocument> findByIdAndOwnerId(String id, String ownerId);

  Flux<AccountDocument> findAllByOwnerId(String ownerId);

  Flux<AccountDocument> findAllByOwnerIdAndIsActiveTrue(String ownerId);

  Flux<AccountDocument> findAllByIsActiveTrue();
}
