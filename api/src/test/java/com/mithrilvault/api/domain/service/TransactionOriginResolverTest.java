package com.mithrilvault.api.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.exception.NotImplementedException;
import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.port.AccountRepository;
import com.mithrilvault.api.fixture.command.transaction.CreateTransactionCommands;
import com.mithrilvault.api.fixture.model.Accounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TransactionOriginResolverTest {

  @Mock private AccountRepository accountRepository;

  private TransactionOriginResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new TransactionOriginResolver(accountRepository);
  }

  @Test
  void resolvesAccountOrigin_whenPaymentMethodTargetsAnAccount() {
    var account = Accounts.checking();
    var command = CreateTransactionCommands.validForAccount(account.id());
    when(accountRepository.findByIdAndOwnerId(eq(account.id()), eq(Accounts.DEFAULT_OWNER_ID)))
        .thenReturn(Mono.just(account));

    StepVerifier.create(resolver.resolve(command, Accounts.DEFAULT_OWNER_ID))
        .assertNext(
            origin -> {
              assertThat(origin.account()).isEqualTo(account);
              assertThat(origin.card()).isNull();
              assertThat(origin.invoice()).isNull();
            })
        .verifyComplete();
  }

  @Test
  void errorsNotFound_whenAccountDoesNotExist() {
    var command = CreateTransactionCommands.validForAccount("missing-account");
    when(accountRepository.findByIdAndOwnerId(eq("missing-account"), eq(Accounts.DEFAULT_OWNER_ID)))
        .thenReturn(Mono.empty());

    StepVerifier.create(resolver.resolve(command, Accounts.DEFAULT_OWNER_ID))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void errorsNotImplemented_forCreditCardPaymentMethod() {
    var command =
        CreateTransactionCommands.withPaymentMethod("account-1", PaymentMethod.CREDIT_CARD);

    StepVerifier.create(resolver.resolve(command, Accounts.DEFAULT_OWNER_ID))
        .expectError(NotImplementedException.class)
        .verify();
  }

  @Test
  void errorsNotImplemented_forDebitCardPaymentMethod() {
    var command =
        CreateTransactionCommands.withPaymentMethod("account-1", PaymentMethod.DEBIT_CARD);

    StepVerifier.create(resolver.resolve(command, Accounts.DEFAULT_OWNER_ID))
        .expectError(NotImplementedException.class)
        .verify();
  }
}
