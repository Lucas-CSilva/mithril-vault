package com.mithrilvault.api.domain.commandhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.command.account.CreateAccountCommands;
import com.mithrilvault.api.fixture.model.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateAccountCommandHandlerTest {

  @Mock private AccountRepository accountRepository;

  private CreateAccountCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CreateAccountCommandHandler(accountRepository);
  }

  @Test
  void savesActiveAccountOwnedByCaller() {
    when(accountRepository.save(any()))
        .thenAnswer(
            inv -> Mono.just(((Account) inv.getArgument(0)).toBuilder().id("new-id").build()));

    StepVerifier.create(handler.handle(Accounts.DEFAULT_OWNER_ID, CreateAccountCommands.valid()))
        .assertNext(
            account -> {
              assertThat(account.name()).isEqualTo(CreateAccountCommands.DEFAULT_NAME);
              assertThat(account.ownerId()).isEqualTo(Accounts.DEFAULT_OWNER_ID);
              assertThat(account.isActive()).isTrue();
              assertThat(account.initialBalance())
                  .isEqualTo(CreateAccountCommands.DEFAULT_INITIAL_BALANCE);
              assertThat(account.currentBalance())
                  .isEqualTo(CreateAccountCommands.DEFAULT_INITIAL_BALANCE);
            })
        .verifyComplete();
  }
}
