package com.mithrilvault.api.infrastructure.adapter.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BalanceProjectionQueuePublisherTest {

  private static final String QUEUE_NAME = "mithril-vault-balance-projection";

  @Mock private SqsTemplate sqsTemplate;

  private BalanceProjectionQueuePublisher publisher;

  private BalanceProjectionMessage message;

  @BeforeEach
  void setUp() {
    publisher = new BalanceProjectionQueuePublisher(sqsTemplate);
    message =
        BalanceProjectionMessage.builder()
            .ownerId("owner-1")
            .transactionId("transaction-1")
            .accountId("account-1")
            .type(com.mithrilvault.api.domain.model.TransactionType.CREDIT)
            .amount(5_000L)
            .target(ProjectionTarget.ACCOUNT)
            .build();
  }

  @Test
  void publish_sendsMessageToBalanceProjectionQueue_andCompletes() {
    SendResult<BalanceProjectionMessage> sendResult = mock(SendResult.class);
    when(sqsTemplate.sendAsync(eq(QUEUE_NAME), eq(message)))
        .thenReturn(CompletableFuture.completedFuture(sendResult));

    StepVerifier.create(publisher.publish(message)).verifyComplete();

    verify(sqsTemplate).sendAsync(QUEUE_NAME, message);
  }

  @Test
  void publish_propagatesError_whenSendFails() {
    CompletableFuture<SendResult<BalanceProjectionMessage>> failed = new CompletableFuture<>();
    failed.completeExceptionally(new RuntimeException("SQS unavailable"));
    when(sqsTemplate.sendAsync(eq(QUEUE_NAME), any(BalanceProjectionMessage.class)))
        .thenReturn(failed);

    StepVerifier.create(publisher.publish(message)).expectError(RuntimeException.class).verify();
  }
}
