package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Card;
import reactor.core.publisher.Mono;

public interface CardReadRepository {

  Mono<Card> findByIdAndOwnerId(String id, String ownerId);
}
