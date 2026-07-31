package com.mithrilvault.api.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.infrastructure.persistence.ProjectionCheckpointMongoRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.test.StepVerifier;

class ProjectionCheckpointRepositoryAdapterIT extends AbstractIntegrationTest {

  // Deliberately distinct from the real "accountBalance" projection name used by the
  // production AccountBalanceChangeStreamListener singleton, which is also live in this
  // shared IT context — reusing its name would corrupt its checkpoint and vice versa.
  private static final String PROJECTION_NAME = "test-checkpoint-projection";

  private static Document resumeToken(String data) {
    return new Document("_data", data);
  }

  @Autowired private ProjectionCheckpointRepositoryAdapter adapter;
  @Autowired private ProjectionCheckpointMongoRepository mongoRepository;
  @Autowired private ReactiveMongoTemplate reactiveMongoTemplate;

  @BeforeEach
  void cleanUp() {
    reactiveMongoTemplate
        .remove(Query.query(Criteria.where("_id").is(PROJECTION_NAME)), "projection_checkpoints")
        .block();
  }

  @Test
  void findResumeToken_returnsEmpty_whenNoCheckpointExists() {
    StepVerifier.create(adapter.findResumeToken(PROJECTION_NAME)).verifyComplete();
  }

  @Test
  void advance_createsCheckpoint_whenNoneExists() {
    Document resumeToken = resumeToken("resume-token-1");

    StepVerifier.create(adapter.advance(PROJECTION_NAME, resumeToken, "transaction-1"))
        .verifyComplete();

    StepVerifier.create(mongoRepository.findById(PROJECTION_NAME))
        .assertNext(
            checkpoint -> {
              assertThat(checkpoint.getResumeToken()).isEqualTo(resumeToken);
              assertThat(checkpoint.getLastProcessedTransactionId()).isEqualTo("transaction-1");
            })
        .verifyComplete();
  }

  @Test
  void advance_updatesExistingCheckpoint_andFindResumeTokenReflectsIt() {
    adapter.advance(PROJECTION_NAME, resumeToken("resume-token-1"), "transaction-1").block();

    Document secondToken = resumeToken("resume-token-2");
    StepVerifier.create(adapter.advance(PROJECTION_NAME, secondToken, "transaction-2"))
        .verifyComplete();

    StepVerifier.create(adapter.findResumeToken(PROJECTION_NAME))
        .assertNext(token -> assertThat(token).isEqualTo(secondToken))
        .verifyComplete();
  }
}
