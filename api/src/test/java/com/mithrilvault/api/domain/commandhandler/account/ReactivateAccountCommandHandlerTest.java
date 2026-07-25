package com.mithrilvault.api.domain.commandhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.model.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactivateAccountCommandHandlerTest {

  @Mock private AccountRepository accountRepository;

  private ReactivateAccountCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ReactivateAccountCommandHandler(accountRepository);
  }

  @Test
  void reactivatesOwnedAccount() {
    Account inactive = Accounts.inactive();
    when(accountRepository.findByIdAndOwnerId(inactive.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(inactive));
    when(accountRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(handler.handle(inactive.id(), Accounts.DEFAULT_OWNER_ID))
        .assertNext(account -> assertThat(account.isActive()).isTrue())
        .verifyComplete();
  }

  @Test
  void throwsNotFoundWhenAccountDoesNotExist() {
    when(accountRepository.findByIdAndOwnerId("ghost", Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.empty());

    StepVerifier.create(handler.handle("ghost", Accounts.DEFAULT_OWNER_ID))
        .expectError(NotFoundException.class)
        .verify();
  }
}
