package com.mithrilvault.api.infrastructure.adapter.projection;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.infrastructure.adapter.messaging.BalanceProjectionMessage;
import com.mithrilvault.api.infrastructure.adapter.messaging.BalanceProjectionQueuePublisher;
import com.mithrilvault.api.infrastructure.adapter.persistence.ProjectionCheckpointRepositoryAdapter;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import com.mongodb.MongoClientSettings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocumentReader;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveChangeStreamOperation;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.projections", name = "enabled", matchIfMissing = true)
public class AccountBalanceChangeStreamListener implements SmartLifecycle {

  private static final String PROJECTION_NAME = "accountBalance";

  private final AppProperties appProperties;
  private final ProjectionLeaderElector leaderElector;
  private final ReactiveMongoTemplate reactiveMongoTemplate;
  private final BalanceProjectionQueuePublisher queuePublisher;
  private final ProjectionCheckpointRepositoryAdapter checkpointRepository;
  private final MeterRegistry meterRegistry;

  private Disposable subscription;
  private String instanceId;
  private Counter replayNoopCounter;

  @PostConstruct
  public void init() {
    try {
      instanceId = InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
    } catch (UnknownHostException e) {
      instanceId = UUID.randomUUID().toString();
    }
    replayNoopCounter =
        Counter.builder("projection.replay.noop.total")
            .tag("projection", PROJECTION_NAME)
            .description("Change-stream events that exhausted retries and were skipped unprojected")
            .register(meterRegistry);
  }

  @Override
  public void start() {
    subscription =
        leaderElector
            .leadershipSignal(PROJECTION_NAME, instanceId, appProperties.leader().ttl())
            .switchMap(isLeader -> isLeader ? changeStreamFlux() : Flux.empty())
            .subscribe();
  }

  private Flux<Void> changeStreamFlux() {
    return Flux.defer(() -> checkpointRepository.findResumeToken(PROJECTION_NAME))
        .flatMap(token -> buildStream(builder -> builder.resumeAfter(toBsonValue(token))))
        .switchIfEmpty(Flux.defer(() -> buildStream(builder -> builder)))
        .concatMap(this::handleEvent);
  }

  private static BsonValue toBsonValue(Document document) {
    return document.toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry());
  }

  private static Document toDocument(BsonValue resumeToken) {
    return new DocumentCodec()
        .decode(new BsonDocumentReader(resumeToken.asDocument()), DecoderContext.builder().build());
  }

  private Flux<ChangeStreamEvent<TransactionDocument>> buildStream(
      Function<
              ReactiveChangeStreamOperation.ChangeStreamWithFilterAndProjection<
                  TransactionDocument>,
              ReactiveChangeStreamOperation.TerminatingChangeStream<TransactionDocument>>
          configurer) {
    var base =
        reactiveMongoTemplate
            .changeStream(TransactionDocument.class)
            .watchCollection("transactions")
            .filter(where("operationType").is("insert"));

    return configurer.apply(base).listen();
  }

  private Mono<Void> handleEvent(ChangeStreamEvent<TransactionDocument> event) {
    String transactionId = event.getBody().getId();
    return Mono.just(event.getBody())
        .doOnNext(
            transaction ->
                log.info(
                    "Processing ChangeStreamEvent of Transaction: {} from user: {}",
                    transaction.getId(),
                    transaction.getOwnerId()))
        .map(BalanceProjectionMessage::of)
        .flatMap(queuePublisher::publish)
        .retryWhen(
            Retry.backoff(
                    appProperties.leader().maxRetries(), appProperties.leader().retryBackoff())
                .doBeforeRetry(
                    signal ->
                        log.warn(
                            "Retrying balance-projection publish for transaction: {} (attempt {}/{}): {}",
                            transactionId,
                            signal.totalRetries() + 1,
                            appProperties.leader().maxRetries(),
                            signal.failure().getMessage())))
        .then(
            checkpointRepository.advance(
                PROJECTION_NAME, toDocument(event.getResumeToken()), event.getBody().getId()))
        .onErrorResume(
            e -> {
              log.error(
                  "Failed to process balance-projection event after retries, checkpoint not advanced: transaction={}",
                  transactionId,
                  e);
              replayNoopCounter.increment();
              return Mono.empty();
            });
  }

  @Override
  public void stop() {
    if (subscription != null) {
      subscription.dispose();
    }
  }

  @Override
  public boolean isRunning() {
    return subscription != null && !subscription.isDisposed();
  }
}
