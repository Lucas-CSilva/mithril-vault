package com.mithrilvault.api.domain.commandhandler.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.domain.port.TransactionReadRepository;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.domain.service.TransactionValidationService;
import com.mithrilvault.api.domain.service.validation.AccountXorCardValidationRule;
import com.mithrilvault.api.fixture.command.transaction.CreateTransactionCommands;
import com.mithrilvault.api.fixture.model.Accounts;
import com.mithrilvault.api.fixture.model.Transactions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateTransferCommandHandlerTest {

  @Mock private AccountRepository accountRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private TransactionReadRepository transactionReadRepository;

  private CreateTransferCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateTransferCommandHandler(
            accountRepository,
            transactionRepository,
            transactionReadRepository,
            new TransactionValidationService(List.of(new AccountXorCardValidationRule())));
  }

  @SuppressWarnings("unchecked")
  @Test
  void createsTwoLegs_whenNoTransferPairIdProvided() {
    var source = Accounts.checking();
    var target = Accounts.checking().toBuilder().id("account-fixture-2").build();
    var command = CreateTransactionCommands.transfer(source.id(), target.id(), null);

    when(accountRepository.findByIdAndOwnerId(source.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(source));
    when(accountRepository.findByIdAndOwnerId(target.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(target));
    when(transactionRepository.saveAll(any()))
        .thenAnswer(inv -> Flux.fromIterable((List<Transaction>) inv.getArgument(0)));

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .assertNext(
            leg -> {
              assertThat(leg.accountId()).isEqualTo(source.id());
              assertThat(leg.type()).isEqualTo(TransactionType.DEBIT);
            })
        .assertNext(
            leg -> {
              assertThat(leg.accountId()).isEqualTo(target.id());
              assertThat(leg.type()).isEqualTo(TransactionType.CREDIT);
            })
        .verifyComplete();

    ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
    verify(transactionRepository).saveAll(captor.capture());
    var legs = captor.getValue();
    assertThat(legs).hasSize(2);
    assertThat(legs.get(0).transferPairId()).isNotBlank().isEqualTo(legs.get(1).transferPairId());
  }

  @Test
  void returnsExistingPair_whenTransferPairIdAlreadyExists() {
    var existingPairId = "pair-1";
    var command = CreateTransactionCommands.transfer("account-1", "account-2", existingPairId);
    var existingLegOne = Transactions.accountTransaction("txn-1");
    var existingLegTwo = Transactions.accountTransaction("txn-2");

    when(transactionReadRepository.findByTransferPairId(Accounts.DEFAULT_OWNER_ID, existingPairId))
        .thenReturn(Flux.just(existingLegOne, existingLegTwo));
    // Stubbed but never actually subscribed: findTransferAccounts(...) is built unconditionally
    // as the switchIfEmpty fallback, so the Mono must exist (a real Mono, not a null default
    // answer) even though the existing-pair branch below short-circuits before it is subscribed.
    when(accountRepository.findByIdAndOwnerId(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .expectNext(existingLegOne, existingLegTwo)
        .verifyComplete();

    verify(transactionRepository, never()).saveAll(any());
  }

  @Test
  void errorsNotFound_whenSourceAccountNotOwned() {
    var command = CreateTransactionCommands.transfer("missing-account", "account-2", null);

    when(accountRepository.findByIdAndOwnerId("missing-account", Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.empty());
    when(accountRepository.findByIdAndOwnerId("account-2", Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(Accounts.checking().toBuilder().id("account-2").build()));

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .expectErrorMatches(
            ex -> ex instanceof NotFoundException && ex.getMessage().contains("Source"))
        .verify();

    verify(transactionRepository, never()).saveAll(any());
  }

  @Test
  void errorsNotFound_whenTargetAccountNotOwned() {
    var command = CreateTransactionCommands.transfer("account-1", "missing-account", null);

    when(accountRepository.findByIdAndOwnerId("account-1", Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(Accounts.checking().toBuilder().id("account-1").build()));
    when(accountRepository.findByIdAndOwnerId("missing-account", Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.empty());

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .expectErrorMatches(
            ex -> ex instanceof NotFoundException && ex.getMessage().contains("Target"))
        .verify();

    verify(transactionRepository, never()).saveAll(any());
  }
}
