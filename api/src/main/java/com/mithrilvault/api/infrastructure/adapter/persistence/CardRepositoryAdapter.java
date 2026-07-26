package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.model.Card;
import com.mithrilvault.api.domain.port.CardReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class CardRepositoryAdapter implements CardReadRepository {

  @Override
  public Mono<Card> findByIdAndOwnerId(String id, String ownerId) {
    // TODO(005-cards): implement once Card persistence exists.
    return Mono.error(new UnsupportedOperationException("Card persistence is not implemented yet"));
  }
}
