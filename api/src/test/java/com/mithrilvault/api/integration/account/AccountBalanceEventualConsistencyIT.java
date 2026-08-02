package com.mithrilvault.api.integration.account;

import com.mithrilvault.api.application.response.AccountResponse;
import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.fixture.command.account.CreateAccountCommands;
import com.mithrilvault.api.fixture.command.transaction.CreateTransactionCommands;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;

/**
 * Exercises the real change-stream → SQS → projector pipeline end to end through the HTTP API:
 * proves a transaction created via {@code POST /transactions} eventually shows up in the owning
 * account's materialized {@code currentBalance}. Off by default (app.projections.enabled=false in
 * the "it" profile) so unrelated *IT tests don't run the listener in the background; this class
 * opts back in, same as {@code AccountBalanceChangeStreamListenerIT}.
 */
@TestPropertySource(properties = "app.projections.enabled=true")
class AccountBalanceEventualConsistencyIT extends AbstractIntegrationTest {

  private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(60);

  @Autowired private AccountMongoRepository accountMongoRepository;
  @Autowired private UserMongoRepository userMongoRepository;
  @Autowired private RefreshTokenMongoRepository refreshTokenMongoRepository;
  @Autowired private ReactiveMongoTemplate reactiveMongoTemplate;

  @BeforeEach
  void setUp() {
    clearCollections();

    String accessToken = userSteps.createAndGetAccessToken();
    accountSteps.setAccessToken(accessToken);
    transactionSteps.setAccessToken(accessToken);

    awaitStreamActive();
  }

  @Test
  void createdTransaction_eventuallyUpdatesAccountCurrentBalance() {
    AccountResponse account = accountSteps.createAndGet(CreateAccountCommands.valid());
    long expectedBalance = account.currentBalance() - CreateTransactionCommands.DEFAULT_AMOUNT;

    transactionSteps.createAndGet(CreateTransactionCommands.validForAccount(account.id()));

    awaitAccountBalance(account.id(), expectedBalance);
  }

  private void clearCollections() {
    accountMongoRepository.deleteAll().block();
    userMongoRepository.deleteAll().block();
    refreshTokenMongoRepository.deleteAll().block();
    reactiveMongoTemplate.remove(new Query(), "transactions").block();
    reactiveMongoTemplate.remove(new Query(), "projection_checkpoints").block();
  }

  private void awaitStreamActive() {
    Instant deadline = Instant.now().plus(AWAIT_TIMEOUT);
    AccountResponse warmup = accountSteps.createAndGet(CreateAccountCommands.valid());
    long expectedBalance = warmup.currentBalance() - CreateTransactionCommands.DEFAULT_AMOUNT;
    transactionSteps.createAndGet(CreateTransactionCommands.validForAccount(warmup.id()));

    while (Instant.now().isBefore(deadline)) {
      if (expectedBalance == currentBalanceOf(warmup.id())) {
        clearCollections();
        return;
      }
      sleep();
    }
    throw new AssertionError("Change stream never became active within " + AWAIT_TIMEOUT);
  }

  private void awaitAccountBalance(String accountId, long expectedBalance) {
    Instant deadline = Instant.now().plus(AWAIT_TIMEOUT);
    long lastSeen = currentBalanceOf(accountId);
    while (Instant.now().isBefore(deadline)) {
      lastSeen = currentBalanceOf(accountId);
      if (expectedBalance == lastSeen) {
        return;
      }
      sleep();
    }
    throw new AssertionError(
        "currentBalance never reached "
            + expectedBalance
            + " within "
            + AWAIT_TIMEOUT
            + ", last seen "
            + lastSeen);
  }

  private long currentBalanceOf(String accountId) {
    return accountSteps
        .get(accountId)
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody()
        .currentBalance();
  }

  private void sleep() {
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
