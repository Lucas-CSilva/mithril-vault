package com.mithrilvault.api.infrastructure.adapter.messaging;

import com.mithrilvault.api.domain.commandhandler.account.ApplyAccountBalanceProjectionCommandHandler;
import com.mithrilvault.api.infrastructure.mapper.ProjectionMessageMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.projections", name = "enabled", matchIfMissing = true)
public class BalanceProjectionListener {

  private final ProjectionMessageMapper projectionMessageMapper;
  private final ApplyAccountBalanceProjectionCommandHandler applyAccountBalanceProjection;

  // Spring Cloud AWS SQS 4.0.0-RC1 has no Reactor support: its listener adapter casts the
  // handler's return value directly to CompletableFuture, so a Mono<Void> here is never
  // subscribed to and silently no-ops. Returning CompletableFuture (via toFuture()) keeps this
  // non-blocking while matching what the framework actually awaits.
  @SqsListener("mithril-vault-balance-projection")
  public CompletableFuture<Void> handle(BalanceProjectionMessage message) {
    return switch (message.target()) {
      case ACCOUNT ->
          applyAccountBalanceProjection
              .handle(projectionMessageMapper.toAccountBalance(message))
              .toFuture();
      case INVOICE -> Mono.<Void>empty().toFuture();
    };
  }
}
