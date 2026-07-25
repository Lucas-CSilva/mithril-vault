package com.mithrilvault.api.domain.queryhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
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
class GetAccountQueryHandlerTest {

  @Mock private AccountRepository accountRepository;

  private GetAccountQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GetAccountQueryHandler(accountRepository);
  }

  @Test
  void returnsOwnedAccount() {
    Account account = Accounts.checking();
    when(accountRepository.findByIdAndOwnerId(account.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(account));

    StepVerifier.create(handler.handle(account.id(), Accounts.DEFAULT_OWNER_ID))
        .assertNext(found -> assertThat(found.id()).isEqualTo(account.id()))
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
