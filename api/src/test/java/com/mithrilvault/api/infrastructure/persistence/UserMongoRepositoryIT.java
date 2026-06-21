package com.mithrilvault.api.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.AbstractIntegrationTest;
import com.mithrilvault.api.domain.model.UserStatus;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.UserDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class UserMongoRepositoryIT extends AbstractIntegrationTest {

  @Autowired private UserMongoRepository userMongoRepository;

  @BeforeEach
  void cleanUp() {
    userMongoRepository.deleteAll().block();
  }

  @Test
  void saveAndFindByEmail() {
    UserDocument doc = UserDocument.builder()
            .email("repo-test@example.com")
            .passwordHash("hashed")
            .displayName("Repo Test")
            .status(UserStatus.ACTIVE)
            .build();

    userMongoRepository.save(doc).block();

    StepVerifier.create(userMongoRepository.findByEmail("repo-test@example.com"))
            .assertNext(found -> assertThat(found.getEmail()).isEqualTo("repo-test@example.com"))
            .verifyComplete();
  }

  @Test
  void findByEmailIsCaseSensitive() {
    UserDocument doc = UserDocument.builder()
            .email("case@example.com")
            .passwordHash("hashed")
            .displayName("Case Test")
            .status(UserStatus.ACTIVE)
            .build();

    userMongoRepository.save(doc).block();

    StepVerifier.create(userMongoRepository.findByEmail("CASE@EXAMPLE.COM"))
            .verifyComplete();
  }

  @Test
  void existsByEmailReturnsFalseWhenAbsent() {
    StepVerifier.create(userMongoRepository.existsByEmail("absent@example.com"))
            .expectNext(false)
            .verifyComplete();
  }
}
