package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository {
  Mono<Account> save(Account account);

  Mono<Account> findByIdAndOwnerId(String id, String ownerId);

  Flux<Account> findAllByOwnerId(String ownerId, boolean includeInactive);
}
