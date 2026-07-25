package com.mithrilvault.api.domain.commandhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.command.account.UpdateAccountCommands;
import com.mithrilvault.api.fixture.model.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UpdateAccountCommandHandlerTest {

  @Mock private AccountRepository accountRepository;

  private UpdateAccountCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new UpdateAccountCommandHandler(accountRepository);
  }

  @Test
  void updatesOwnedAccount() {
    Account existing = Accounts.checking();
    when(accountRepository.findByIdAndOwnerId(existing.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(existing));
    when(accountRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(
            handler.handle(
                existing.id(), Accounts.DEFAULT_OWNER_ID, UpdateAccountCommands.withName()))
        .assertNext(
            account -> assertThat(account.name()).isEqualTo(UpdateAccountCommands.UPDATED_NAME))
        .verifyComplete();
  }

  @Test
  void throwsNotFoundWhenAccountDoesNotExist() {
    when(accountRepository.findByIdAndOwnerId("ghost", Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle("ghost", Accounts.DEFAULT_OWNER_ID, UpdateAccountCommands.withName()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void throwsNotFoundWhenAccountBelongsToAnotherOwner() {
    Account other = Accounts.checking(Accounts.OTHER_OWNER_ID);
    when(accountRepository.findByIdAndOwnerId(other.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            handler.handle(other.id(), Accounts.DEFAULT_OWNER_ID, UpdateAccountCommands.withName()))
        .expectError(NotFoundException.class)
        .verify();
  }
}
