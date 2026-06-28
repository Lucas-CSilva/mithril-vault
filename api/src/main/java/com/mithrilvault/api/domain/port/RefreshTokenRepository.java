package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.RefreshToken;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository {

  Mono<RefreshToken> save(RefreshToken token);

  Mono<RefreshToken> findByTokenHash(String tokenHash);

  Mono<Void> revokeAllByUserId(String userId);
}
