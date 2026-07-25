package com.mithrilvault.api.domain.queryhandler.account;

import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.model.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ListAccountQueryHandlerTest {

  @Mock private AccountRepository accountRepository;

  private ListAccountQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ListAccountQueryHandler(accountRepository);
  }

  @Test
  void delegatesIncludeInactiveFlagToRepository() {
    Account account = Accounts.checking();
    when(accountRepository.findAllByOwnerId(Accounts.DEFAULT_OWNER_ID, true))
        .thenReturn(Flux.just(account));

    StepVerifier.create(handler.handle(Accounts.DEFAULT_OWNER_ID, true))
        .expectNext(account)
        .verifyComplete();
  }

  @Test
  void returnsEmptyFluxWhenNoAccounts() {
    when(accountRepository.findAllByOwnerId(Accounts.DEFAULT_OWNER_ID, false))
        .thenReturn(Flux.empty());

    StepVerifier.create(handler.handle(Accounts.DEFAULT_OWNER_ID, false)).verifyComplete();
  }
}
