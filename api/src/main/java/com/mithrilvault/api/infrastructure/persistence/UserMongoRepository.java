package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.UserDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserMongoRepository extends ReactiveMongoRepository<UserDocument, String> {

  Mono<UserDocument> findByEmailIgnoreCase(String email);

  Mono<Boolean> existsByEmailIgnoreCase(String email);
}
