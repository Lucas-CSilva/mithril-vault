package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.domain.port.RefreshTokenRepository;
import com.mithrilvault.api.infrastructure.mapper.RefreshTokenMapper;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

  private final RefreshTokenMongoRepository mongoRepository;
  private final RefreshTokenMapper refreshTokenMapper;

  @Override
  public Mono<RefreshToken> save(RefreshToken token) {
    return mongoRepository
        .save(refreshTokenMapper.toDocument(token))
        .map(refreshTokenMapper::toDomain);
  }

  @Override
  public Mono<RefreshToken> findByTokenHash(String tokenHash) {
    return mongoRepository.findByTokenHash(tokenHash).map(refreshTokenMapper::toDomain);
  }

  @Override
  public Mono<Void> revokeAllByUserId(String userId) {
    return mongoRepository
        .findAllByUserId(userId)
        .filter(doc -> doc.getRevokedAt() == null)
        .map(
            doc -> {
              doc.setRevokedAt(Instant.now());
              return doc;
            })
        .flatMap(mongoRepository::save)
        .then();
  }
}
