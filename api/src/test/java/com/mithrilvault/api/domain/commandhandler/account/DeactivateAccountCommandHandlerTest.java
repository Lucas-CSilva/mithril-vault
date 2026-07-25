package com.mithrilvault.api.domain.commandhandler.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.model.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeactivateAccountCommandHandlerTest {

  @Mock private AccountRepository accountRepository;

  private DeactivateAccountCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DeactivateAccountCommandHandler(accountRepository);
  }

  @Test
  void deactivatesOwnedAccountPreservingOtherFields() {
    Account existing = Accounts.checking();
    when(accountRepository.findByIdAndOwnerId(existing.id(), Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(existing));
    when(accountRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(handler.handle(existing.id(), Accounts.DEFAULT_OWNER_ID)).verifyComplete();

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    verify(accountRepository).save(captor.capture());
    Account saved = captor.getValue();
    assertThat(saved.isActive()).isFalse();
    assertThat(saved.createdAt()).isEqualTo(existing.createdAt());
    assertThat(saved.name()).isEqualTo(existing.name());
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
