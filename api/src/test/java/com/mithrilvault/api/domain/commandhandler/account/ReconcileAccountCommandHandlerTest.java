package com.mithrilvault.api.domain.commandhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.command.account.ReconcileAccountCommands;
import com.mithrilvault.api.fixture.model.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReconcileAccountCommandHandlerTest {

  @Mock private AccountRepository accountRepository;
  @Mock private AccountReadRepository accountReadRepository;

  private ReconcileAccountCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ReconcileAccountCommandHandler(accountRepository, accountReadRepository);
  }

  @Test
  void adjustInitialBalance_setsInitialBalanceSoCurrentBalanceMatchesRealBalance() {
    Account existing = Accounts.checking();
    when(accountRepository.findByIdAndOwnerId(existing.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(existing));
    when(accountReadRepository.currentBalance(
            existing.id(), existing.ownerId(), existing.initialBalance()))
        .thenReturn(Mono.just(existing.initialBalance()));
    when(accountRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(
            handler.handle(
                existing.id(),
                Accounts.DEFAULT_OWNER_ID,
                ReconcileAccountCommands.adjustInitialBalance()))
        .assertNext(
            account -> {
              assertThat(account.initialBalance())
                  .isEqualTo(ReconcileAccountCommands.DEFAULT_REAL_BALANCE);
              assertThat(account.currentBalance())
                  .isEqualTo(ReconcileAccountCommands.DEFAULT_REAL_BALANCE);
            })
        .verifyComplete();
  }

  @Test
  void adjustInitialBalance_accountsForExistingDiscrepancyBetweenInitialAndCurrentBalance() {
    Account existing = Accounts.checking();
    long currentBalance = existing.initialBalance() + 5_000L;
    when(accountRepository.findByIdAndOwnerId(existing.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(existing));
    when(accountReadRepository.currentBalance(
            existing.id(), existing.ownerId(), existing.initialBalance()))
        .thenReturn(Mono.just(currentBalance));
    when(accountRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(
            handler.handle(
                existing.id(),
                Accounts.DEFAULT_OWNER_ID,
                ReconcileAccountCommands.adjustInitialBalance()))
        .assertNext(
            account -> {
              assertThat(account.initialBalance())
                  .isEqualTo(
                      existing.initialBalance()
                          + (ReconcileAccountCommands.DEFAULT_REAL_BALANCE - currentBalance));
              assertThat(account.currentBalance())
                  .isEqualTo(ReconcileAccountCommands.DEFAULT_REAL_BALANCE);
            })
        .verifyComplete();
  }

  @Test
  void adjustingTransaction_rejectsWithBusinessException() {
    Account existing = Accounts.checking();
    when(accountRepository.findByIdAndOwnerId(existing.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(existing));

    StepVerifier.create(
            handler.handle(
                existing.id(),
                Accounts.DEFAULT_OWNER_ID,
                ReconcileAccountCommands.adjustingTransaction()))
        .expectError(BusinessException.class)
        .verify();

    verify(accountRepository, never()).save(any());
  }

  @Test
  void throwsNotFoundWhenAccountDoesNotExist() {
    when(accountRepository.findByIdAndOwnerId("ghost", Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle(
                "ghost",
                Accounts.DEFAULT_OWNER_ID,
                ReconcileAccountCommands.adjustInitialBalance()))
        .expectError(NotFoundException.class)
        .verify();
  }
}
