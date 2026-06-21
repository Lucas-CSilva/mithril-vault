package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.RefreshTokenDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RefreshTokenMongoRepository
    extends ReactiveMongoRepository<RefreshTokenDocument, String> {

  Mono<RefreshTokenDocument> findByTokenHash(String tokenHash);

  Flux<RefreshTokenDocument> findAllByUserId(String userId);
}
