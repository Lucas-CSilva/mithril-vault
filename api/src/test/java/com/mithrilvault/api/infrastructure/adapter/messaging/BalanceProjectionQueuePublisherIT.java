package com.mithrilvault.api.infrastructure.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.model.TransactionType;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.localstack.LocalStackContainer;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

class BalanceProjectionQueuePublisherIT extends AbstractIntegrationTest {

  private static final String QUEUE_NAME = "mithril-vault-balance-projection";

  private static SqsClient rawSqsClient;
  private static String queueUrl;

  @BeforeAll
  static void createQueue() {
    rawSqsClient =
        SqsClient.builder()
            .endpointOverride(
                URI.create(
                    localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString()))
            .region(Region.of(localstack.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .build();
    queueUrl =
        rawSqsClient
            .createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build())
            .queueUrl();
  }

  @Autowired private BalanceProjectionQueuePublisher publisher;

  @Test
  void publish_deliversMessageToRealSqsQueue() {
    BalanceProjectionMessage message =
        BalanceProjectionMessage.builder()
            .ownerId("owner-1")
            .transactionId("transaction-localstack-1")
            .accountId("account-1")
            .type(TransactionType.CREDIT)
            .amount(2_500L)
            .target(ProjectionTarget.ACCOUNT)
            .build();

    StepVerifier.create(publisher.publish(message)).verifyComplete();

    var received =
        rawSqsClient.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds((int) Duration.ofSeconds(5).toSeconds())
                .maxNumberOfMessages(1)
                .build());

    assertThat(received.messages()).hasSize(1);
    assertThat(received.messages().get(0).body()).contains("transaction-localstack-1");
  }
}
