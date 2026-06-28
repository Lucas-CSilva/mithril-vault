package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.User;
import reactor.core.publisher.Mono;

public interface UserRepository {

  Mono<User> save(User user);

  Mono<Boolean> existsByEmail(String email);

  Mono<User> findByEmail(String email);

  Mono<User> findById(String id);
}
