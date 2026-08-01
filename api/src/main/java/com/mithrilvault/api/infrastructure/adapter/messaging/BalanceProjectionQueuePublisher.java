package com.mithrilvault.api.infrastructure.adapter.messaging;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceProjectionQueuePublisher {

  private final SqsTemplate sqsTemplate;

  private static final String QUEUE_NAME = "mithril-vault-balance-projection";

  public Mono<Void> publish(BalanceProjectionMessage message) {
    log.info(
        "Publishing BalanceProjection message with id: {} for transaction: {} from user: {}",
        message.id(),
        message.transactionId(),
        message.ownerId());
    return Mono.fromFuture(sqsTemplate.sendAsync(QUEUE_NAME, message))
        .doOnSuccess(
            result ->
                log.info(
                    "Published BalanceProjection message with id: {} for transaction: {} from user: {}",
                    message.id(),
                    message.transactionId(),
                    message.ownerId()))
        .then();
  }
}
