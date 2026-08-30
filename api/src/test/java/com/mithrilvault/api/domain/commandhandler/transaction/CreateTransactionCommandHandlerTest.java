package com.mithrilvault.api.domain.commandhandler.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionOrigin;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.domain.service.TransactionOriginResolver;
import com.mithrilvault.api.domain.service.TransactionValidationService;
import com.mithrilvault.api.domain.service.validation.AccountXorCardValidationRule;
import com.mithrilvault.api.fixture.command.transaction.CreateTransactionCommands;
import com.mithrilvault.api.fixture.model.Accounts;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateTransactionCommandHandlerTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private TransactionOriginResolver originResolver;

  private CreateTransactionCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateTransactionCommandHandler(
            transactionRepository,
            originResolver,
            new TransactionValidationService(List.of(new AccountXorCardValidationRule())));
  }

  @Test
  void savesTransaction_whenValidationSucceeds() {
    var account = Accounts.checking();
    var command = CreateTransactionCommands.validForAccount(account.id());
    when(originResolver.resolve(command, Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(new TransactionOrigin(account, null, null)));
    when(transactionRepository.save(any()))
        .thenAnswer(
            inv -> Mono.just(((Transaction) inv.getArgument(0)).toBuilder().id("txn-1").build()));

    // Regression guard for the flatMap-on-empty-Mono bug: previously
    // `.flatMap(ignored -> originResolver.resolve(...))` never fired because
    // TransactionValidationService.validate() completes empty (a Mono<Void>) on success, so the
    // whole chain silently no-op'd instead of resolving the origin and saving.
    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .assertNext(
            transaction -> {
              assertThat(transaction.id()).isEqualTo("txn-1");
              assertThat(transaction.accountId()).isEqualTo(account.id());
            })
        .verifyComplete();

    verify(transactionRepository).save(any());
  }

  @Test
  void doesNotResolveOriginOrSave_whenValidationFails() {
    var command = CreateTransactionCommands.withBothAccountAndCard("account-1", "card-1");

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .expectError(BusinessException.class)
        .verify();

    verify(originResolver, never()).resolve(any(), any());
    verify(transactionRepository, never()).save(any());
  }
}
