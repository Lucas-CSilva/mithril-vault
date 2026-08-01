package com.mithrilvault.api.infrastructure.adapter.messaging;

import com.mithrilvault.api.domain.commandhandler.account.ApplyAccountBalanceProjectionCommandHandler;
import com.mithrilvault.api.infrastructure.mapper.ProjectionMessageMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
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

  @SqsListener("mithril-vault-balance-projection")
  public Mono<Void> handle(BalanceProjectionMessage message) {
    return switch (message.target()) {
      case ACCOUNT ->
          applyAccountBalanceProjection.handle(projectionMessageMapper.toAccountBalance(message));
      case INVOICE -> Mono.empty();
    };
  }
}
