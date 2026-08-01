package com.mithrilvault.api.domain.commandhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.command.account.ApplyAccountBalanceProjectionCommand;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.domain.port.ProjectionRepository;
import com.mithrilvault.api.domain.port.TransactionReadRepository;
import com.mithrilvault.api.fixture.model.Accounts;
import com.mithrilvault.api.fixture.model.Transactions;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ApplyAccountBalanceProjectionCommandHandlerTest {

  @Mock private ProjectionRepository projectionRepository;
  @Mock private TransactionReadRepository transactionReadRepository;

  @Captor private ArgumentCaptor<Transaction> transactionCaptor;

  private ApplyAccountBalanceProjectionCommandHandler newHandler() {
    return new ApplyAccountBalanceProjectionCommandHandler(
        projectionRepository, transactionReadRepository);
  }

  private static ApplyAccountBalanceProjectionCommand command(TransactionType type, long amount) {
    return new ApplyAccountBalanceProjectionCommand(
        Accounts.DEFAULT_OWNER_ID,
        Transactions.accountTransaction().id(),
        Accounts.checking().id(),
        type,
        amount,
        "projection-1");
  }

  @Test
  void appliesProjection_andMarksItOnTheTransaction_whenNotYetApplied() {
    var transaction = Transactions.accountTransaction();
    var command = command(TransactionType.CREDIT, 500L);

    when(transactionReadRepository.findByIdAndOwnerId(command.transactionId(), command.ownerId()))
        .thenReturn(Mono.just(transaction));
    when(projectionRepository.markAppliedAndUpdateBalance(
            eq(command.projectionId()),
            eq(command.ownerId()),
            eq(command.accountId()),
            eq(500L),
            transactionCaptor.capture()))
        .thenReturn(Mono.empty());

    StepVerifier.create(newHandler().handle(command)).verifyComplete();

    assertThat(transactionCaptor.getValue().appliedProjections())
        .containsExactly(command.projectionId());
  }

  @Test
  void computesNegativeSignedAmount_forDebit() {
    var transaction = Transactions.accountTransaction();
    var command = command(TransactionType.DEBIT, 500L);

    when(transactionReadRepository.findByIdAndOwnerId(command.transactionId(), command.ownerId()))
        .thenReturn(Mono.just(transaction));
    when(projectionRepository.markAppliedAndUpdateBalance(
            eq(command.projectionId()),
            eq(command.ownerId()),
            eq(command.accountId()),
            eq(-500L),
            any()))
        .thenReturn(Mono.empty());

    StepVerifier.create(newHandler().handle(command)).verifyComplete();

    verify(projectionRepository)
        .markAppliedAndUpdateBalance(
            eq(command.projectionId()),
            eq(command.ownerId()),
            eq(command.accountId()),
            eq(-500L),
            any());
  }

  @Test
  void skipsWithoutCallingRepository_whenProjectionAlreadyApplied() {
    var command = command(TransactionType.CREDIT, 500L);
    var alreadyApplied =
        Transactions.accountTransaction().toBuilder()
            .appliedProjections(Set.of(command.projectionId()))
            .build();

    when(transactionReadRepository.findByIdAndOwnerId(command.transactionId(), command.ownerId()))
        .thenReturn(Mono.just(alreadyApplied));

    StepVerifier.create(newHandler().handle(command)).verifyComplete();

    verify(projectionRepository, never())
        .markAppliedAndUpdateBalance(any(), any(), any(), any(), any());
  }

  @Test
  void skipsWithoutCallingRepository_whenTransactionNotFound() {
    var command = command(TransactionType.CREDIT, 500L);

    when(transactionReadRepository.findByIdAndOwnerId(command.transactionId(), command.ownerId()))
        .thenReturn(Mono.empty());

    StepVerifier.create(newHandler().handle(command)).verifyComplete();

    verify(projectionRepository, never())
        .markAppliedAndUpdateBalance(any(), any(), any(), any(), any());
  }

  @Test
  void preservesExistingAppliedProjections_whenAddingANewOne() {
    var transaction =
        Transactions.accountTransaction().toBuilder()
            .appliedProjections(Set.of("other-projection"))
            .build();
    var command = command(TransactionType.CREDIT, 500L);

    when(transactionReadRepository.findByIdAndOwnerId(command.transactionId(), command.ownerId()))
        .thenReturn(Mono.just(transaction));
    when(projectionRepository.markAppliedAndUpdateBalance(
            eq(command.projectionId()),
            eq(command.ownerId()),
            eq(command.accountId()),
            eq(500L),
            transactionCaptor.capture()))
        .thenReturn(Mono.empty());

    StepVerifier.create(newHandler().handle(command)).verifyComplete();

    assertThat(transactionCaptor.getValue().appliedProjections())
        .containsExactlyInAnyOrder("other-projection", command.projectionId());
  }
}
