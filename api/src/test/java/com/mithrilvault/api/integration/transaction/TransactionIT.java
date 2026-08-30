package com.mithrilvault.api.integration.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.application.response.AccountResponse;
import com.mithrilvault.api.application.response.TransactionResponse;
import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.fixture.command.account.CreateAccountCommands;
import com.mithrilvault.api.fixture.command.transaction.CreateTransactionCommands;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.TransactionMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TransactionIT extends AbstractIntegrationTest {

  @Autowired private AccountMongoRepository accountMongoRepository;
  @Autowired private TransactionMongoRepository transactionMongoRepository;
  @Autowired private UserMongoRepository userMongoRepository;
  @Autowired private RefreshTokenMongoRepository refreshTokenMongoRepository;

  @BeforeEach
  void setUp() {
    transactionMongoRepository.deleteAll().block();
    accountMongoRepository.deleteAll().block();
    userMongoRepository.deleteAll().block();
    refreshTokenMongoRepository.deleteAll().block();

    String accessToken = userSteps.createAndGetAccessToken();
    accountSteps.setAccessToken(accessToken);
    transactionSteps.setAccessToken(accessToken);
  }

  @Test
  void createTransaction_returns201_forValidAccountBasedTransaction() {
    AccountResponse account = accountSteps.createAndGet(CreateAccountCommands.valid());

    transactionSteps
        .create(CreateTransactionCommands.validForAccount(account.id()))
        .expectStatus()
        .isCreated()
        .expectBodyList(TransactionResponse.class)
        .hasSize(1)
        .value(
            body -> {
              TransactionResponse transaction = body.get(0);
              assertThat(transaction.id()).isNotNull();
              assertThat(transaction.accountId()).isEqualTo(account.id());
              assertThat(transaction.amount()).isEqualTo(CreateTransactionCommands.DEFAULT_AMOUNT);
              assertThat(transaction.description())
                  .isEqualTo(CreateTransactionCommands.DEFAULT_DESCRIPTION);
              assertThat(transaction.createdAt()).isNotNull();
            });

    assertThat(transactionMongoRepository.count().block()).isEqualTo(1);
  }

  @Test
  void createTransaction_returns404_whenAccountBelongsToAnotherOwner() {
    AccountDocument otherOwnersAccount =
        accountMongoRepository
            .save(
                AccountDocument.builder()
                    .name("Other's Account")
                    .ownerId("completely-different-owner")
                    .initialBalance(1000L)
                    .isActive(true)
                    .build())
            .block();

    transactionSteps
        .create(CreateTransactionCommands.validForAccount(otherOwnersAccount.getId()))
        .expectStatus()
        .isNotFound();
  }

  @Test
  void createTransaction_returns422_whenBothAccountIdAndCardIdAreSet() {
    AccountResponse account = accountSteps.createAndGet(CreateAccountCommands.valid());

    transactionSteps
        .create(CreateTransactionCommands.withBothAccountAndCard(account.id(), "card-1"))
        .expectStatus()
        .isEqualTo(422);
  }

  @Test
  void createTransaction_returns501_forCreditCardPaymentMethod() {
    AccountResponse account = accountSteps.createAndGet(CreateAccountCommands.valid());

    transactionSteps
        .create(
            CreateTransactionCommands.withPaymentMethod(account.id(), PaymentMethod.CREDIT_CARD))
        .expectStatus()
        .isEqualTo(501);
  }

  @Test
  void createTransaction_returns401_whenUnauthenticated() {
    webTestClient
        .post()
        .uri("/mithril-vault/transactions")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(CreateTransactionCommands.validForAccount("some-account-id"))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
