package com.mithrilvault.api.domain.queryhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalancePoint;
import com.mithrilvault.api.domain.port.AccountReadRepository;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.model.Accounts;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetAccountBalanceHistoryQueryHandlerTest {

  @Mock private AccountRepository accountRepository;
  @Mock private AccountReadRepository accountReadRepository;

  private GetAccountBalanceHistoryQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GetAccountBalanceHistoryQueryHandler(accountRepository, accountReadRepository);
  }

  @Test
  void returnsPointsFromReadRepositoryForOwnedAccount() {
    Account account = Accounts.checking();
    BalancePoint point = new BalancePoint(LocalDate.now(), account.currentBalance());
    when(accountRepository.findByIdAndOwnerId(account.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(account));
    when(accountReadRepository.balanceHistory(
            account.id(), Accounts.DEFAULT_OWNER_ID, account.currentBalance(), 30))
        .thenReturn(Flux.just(point));

    StepVerifier.create(handler.handle(account.id(), Accounts.DEFAULT_OWNER_ID))
        .assertNext(p -> assertThat(p).isEqualTo(point))
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
