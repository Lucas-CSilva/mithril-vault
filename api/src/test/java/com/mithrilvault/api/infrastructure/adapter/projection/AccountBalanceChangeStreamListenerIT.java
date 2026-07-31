package com.mithrilvault.api.infrastructure.adapter.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.infrastructure.adapter.messaging.BalanceProjectionQueuePublisher;
import com.mithrilvault.api.infrastructure.persistence.ProjectionCheckpointMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.ProjectionCheckpointDocument;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

/**
 * The listener under test is the real singleton {@code AccountBalanceChangeStreamListener} bean.
 * It's off by default in the "it" profile (app.projections.enabled=false) so unrelated *IT tests
 * don't run it in the background; this class opts back in. Only its {@link
 * BalanceProjectionQueuePublisher} collaborator is mocked; leader election and the Mongo change
 * stream run for real against the Testcontainers replica set.
 */
@TestPropertySource(properties = "app.projections.enabled=true")
class AccountBalanceChangeStreamListenerIT extends AbstractIntegrationTest {

  private static final String PROJECTION_NAME = "accountBalance";
  private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);

  @MockitoBean private BalanceProjectionQueuePublisher queuePublisher;

  @Autowired private ReactiveMongoTemplate reactiveMongoTemplate;
  @Autowired private ProjectionCheckpointMongoRepository checkpointMongoRepository;
  @Autowired private MeterRegistry meterRegistry;

  @BeforeEach
  void cleanUp() {
    clearCollections();
    reset(queuePublisher);
    when(queuePublisher.publish(any())).thenReturn(Mono.empty());
    awaitStreamActive();
    clearCollections();
    reset(queuePublisher);
  }

  @Test
  void publishesMessageAndAdvancesCheckpoint_forInsertedTransaction() {
    when(queuePublisher.publish(any())).thenReturn(Mono.empty());

    TransactionDocument inserted = insertTransaction();

    verify(queuePublisher, timeout(AWAIT_TIMEOUT.toMillis()))
        .publish(argThat(msg -> msg.transactionId().equals(inserted.getId())));

    awaitCheckpointAdvancedTo(inserted.getId());
  }

  @Test
  void streamSurvivesAFailedEvent_andKeepsProcessingSubsequentTransactions() {
    when(queuePublisher.publish(any())).thenReturn(Mono.empty());
    double failuresBefore = replayNoopCount();

    String poisonId = "poison-" + UUID.randomUUID();
    when(queuePublisher.publish(
            argThat(msg -> msg != null && msg.transactionId().equals(poisonId))))
        .thenReturn(Mono.error(new RuntimeException("SQS unavailable")));
    insertTransaction(poisonId);

    TransactionDocument healthy = insertTransaction();

    // A generous wait: proves the stream survived the poison event's exhausted retries
    // (previously it would have died there under the old .doOnError-without-swallow behavior)
    // and kept processing the next, unrelated transaction.
    awaitCheckpointAdvancedTo(healthy.getId());

    assertThat(replayNoopCount()).isEqualTo(failuresBefore + 1);
  }

  private void clearCollections() {
    reactiveMongoTemplate.remove(new Query(), "transactions").block();
    reactiveMongoTemplate
        .remove(Query.query(Criteria.where("_id").is(PROJECTION_NAME)), "projection_checkpoints")
        .block();
  }

  private void awaitStreamActive() {
    Instant deadline = Instant.now().plus(AWAIT_TIMEOUT);
    AssertionError lastFailure = null;
    while (Instant.now().isBefore(deadline)) {
      TransactionDocument warmup = insertTransaction();
      try {
        verify(queuePublisher, timeout(1_000))
            .publish(argThat(msg -> msg.transactionId().equals(warmup.getId())));
        return;
      } catch (AssertionError notYetLive) {
        lastFailure = notYetLive;
      }
    }
    throw new AssertionError(
        "Change stream never became active within " + AWAIT_TIMEOUT, lastFailure);
  }

  private TransactionDocument insertTransaction() {
    return insertTransaction("tx-" + UUID.randomUUID());
  }

  private TransactionDocument insertTransaction(String id) {
    TransactionDocument transaction =
        TransactionDocument.builder()
            .id(id)
            .ownerId("owner-projection-it")
            .accountId("account-projection-it")
            .type(TransactionType.CREDIT)
            .amount(1_000L)
            .date(LocalDate.now())
            .build();
    return reactiveMongoTemplate.insert(transaction, "transactions").block();
  }

  private double replayNoopCount() {
    var counter = meterRegistry.find("projection.replay.noop.total").counter();
    return counter == null ? 0.0 : counter.count();
  }

  private void awaitCheckpointAdvancedTo(String transactionId) {
    Instant deadline = Instant.now().plus(AWAIT_TIMEOUT);
    ProjectionCheckpointDocument checkpoint = null;
    while (Instant.now().isBefore(deadline)) {
      checkpoint = checkpointMongoRepository.findById(PROJECTION_NAME).blockOptional().orElse(null);
      if (checkpoint != null && transactionId.equals(checkpoint.getLastProcessedTransactionId())) {
        return;
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    throw new AssertionError(
        "Checkpoint did not advance to transaction "
            + transactionId
            + " within "
            + AWAIT_TIMEOUT
            + ", last seen: "
            + checkpoint);
  }
}
