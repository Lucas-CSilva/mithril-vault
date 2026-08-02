package com.mithrilvault.api.infrastructure.adapter.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.command.account.ApplyAccountBalanceProjectionCommand;
import com.mithrilvault.api.domain.commandhandler.account.ApplyAccountBalanceProjectionCommandHandler;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.infrastructure.mapper.ProjectionMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BalanceProjectionListenerTest {

  @Mock private ProjectionMessageMapper projectionMessageMapper;
  @Mock private ApplyAccountBalanceProjectionCommandHandler applyAccountBalanceProjection;

  private BalanceProjectionListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new BalanceProjectionListener(projectionMessageMapper, applyAccountBalanceProjection);
  }

  private static BalanceProjectionMessage message(ProjectionTarget target) {
    return BalanceProjectionMessage.builder()
        .id("message-1")
        .ownerId("owner-1")
        .transactionId("transaction-1")
        .accountId("account-1")
        .type(TransactionType.CREDIT)
        .amount(500L)
        .target(target)
        .build();
  }

  @Test
  void delegatesToHandler_forAccountTargetMessages() {
    var message = message(ProjectionTarget.ACCOUNT);
    var command =
        new ApplyAccountBalanceProjectionCommand(
            "owner-1", "transaction-1", "account-1", TransactionType.CREDIT, 500L, "message-1");

    when(projectionMessageMapper.toAccountBalance(message)).thenReturn(command);
    when(applyAccountBalanceProjection.handle(command)).thenReturn(Mono.empty());

    StepVerifier.create(Mono.fromFuture(listener.handle(message))).verifyComplete();

    verify(applyAccountBalanceProjection).handle(command);
  }

  @Test
  void doesNothing_forInvoiceTargetMessages() {
    var message = message(ProjectionTarget.INVOICE);

    StepVerifier.create(Mono.fromFuture(listener.handle(message))).verifyComplete();

    verify(applyAccountBalanceProjection, never()).handle(any());
  }
}
