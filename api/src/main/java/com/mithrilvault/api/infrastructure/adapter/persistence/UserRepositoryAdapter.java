package com.mithrilvault.api.infrastructure.adapter;

import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.domain.port.UserRepository;
import com.mithrilvault.api.infrastructure.mapper.UserMapper;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

  private final UserMongoRepository mongoRepository;
  private final UserMapper userMapper;

  @Override
  public Mono<User> save(User user) {
    return mongoRepository.save(userMapper.toDocument(user)).map(userMapper::toDomain);
  }

  @Override
  public Mono<Boolean> existsByEmail(String email) {
    return mongoRepository.existsByEmail(email);
  }

  @Override
  public Mono<User> findByEmail(String email) {
    return mongoRepository.findByEmail(email).map(userMapper::toDomain);
  }

  @Override
  public Mono<User> findById(String id) {
    return mongoRepository.findById(id).map(userMapper::toDomain);
  }
}
