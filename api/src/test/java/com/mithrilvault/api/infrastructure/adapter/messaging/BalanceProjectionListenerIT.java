package com.mithrilvault.api.infrastructure.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.commandhandler.account.ApplyAccountBalanceProjectionCommandHandler;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.fixture.model.Accounts;
import com.mithrilvault.api.infrastructure.mapper.ProjectionMessageMapper;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.TransactionMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

/**
 * Exercises the real {@link BalanceProjectionListener} bean and its collaborators end-to-end
 * against a real Mongo replica set, bypassing the SQS transport itself (per
 * specs/003-accounts/implementation-notes.md §11.12: "publish the same BalanceProjectionMessage
 * twice directly against the listener... doesn't need real SQS redelivery to prove the guard
 * works"). Real SQS publish/consume is covered separately by {@link
 * BalanceProjectionQueuePublisherIT}.
 */
class BalanceProjectionListenerIT extends AbstractIntegrationTest {

  @Autowired private ProjectionMessageMapper projectionMessageMapper;
  @Autowired private ApplyAccountBalanceProjectionCommandHandler applyAccountBalanceProjection;
  @Autowired private AccountMongoRepository accountMongoRepository;
  @Autowired private TransactionMongoRepository transactionMongoRepository;

  private BalanceProjectionListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new BalanceProjectionListener(projectionMessageMapper, applyAccountBalanceProjection);
    accountMongoRepository.deleteAll().block();
    transactionMongoRepository.deleteAll().block();
  }

  private AccountDocument seedAccount() {
    return accountMongoRepository
        .save(
            AccountDocument.builder()
                .ownerId(Accounts.DEFAULT_OWNER_ID)
                .name("Nubank")
                .institution("Nubank")
                .initialBalance(1_000L)
                .currentBalance(1_000L)
                .color("#88C0D0")
                .isActive(true)
                .build())
        .block();
  }

  private TransactionDocument seedTransaction(String accountId) {
    return transactionMongoRepository
        .save(
            TransactionDocument.builder()
                .ownerId(Accounts.DEFAULT_OWNER_ID)
                .type(TransactionType.CREDIT)
                .amount(500L)
                .date(LocalDate.parse("2026-01-01"))
                .accountId(accountId)
                .appliedProjections(Set.of())
                .build())
        .block();
  }

  @Test
  void appliesTheProjection_andIncrementsTheAccountBalance() {
    AccountDocument account = seedAccount();
    TransactionDocument transaction = seedTransaction(account.getId());

    var message =
        BalanceProjectionMessage.builder()
            .id("projection-1")
            .ownerId(Accounts.DEFAULT_OWNER_ID)
            .transactionId(transaction.getId())
            .accountId(account.getId())
            .type(TransactionType.CREDIT)
            .amount(500L)
            .target(ProjectionTarget.ACCOUNT)
            .build();

    StepVerifier.create(listener.handle(message)).verifyComplete();

    AccountDocument reloadedAccount = accountMongoRepository.findById(account.getId()).block();
    assertThat(reloadedAccount.getCurrentBalance()).isEqualTo(1_500L);

    TransactionDocument reloadedTransaction =
        transactionMongoRepository.findById(transaction.getId()).block();
    assertThat(reloadedTransaction.getAppliedProjections()).containsExactly("projection-1");
  }

  @Test
  void isIdempotent_whenTheSameMessageIsDeliveredTwice() {
    AccountDocument account = seedAccount();
    TransactionDocument transaction = seedTransaction(account.getId());

    var message =
        BalanceProjectionMessage.builder()
            .id("projection-1")
            .ownerId(Accounts.DEFAULT_OWNER_ID)
            .transactionId(transaction.getId())
            .accountId(account.getId())
            .type(TransactionType.CREDIT)
            .amount(500L)
            .target(ProjectionTarget.ACCOUNT)
            .build();

    // Simulates SQS at-least-once redelivery: the exact same message is handled twice.
    StepVerifier.create(listener.handle(message)).verifyComplete();
    StepVerifier.create(listener.handle(message)).verifyComplete();

    AccountDocument reloadedAccount = accountMongoRepository.findById(account.getId()).block();
    assertThat(reloadedAccount.getCurrentBalance()).isEqualTo(1_500L);
  }

  @Test
  void doesNothing_forInvoiceTargetMessages() {
    var message =
        BalanceProjectionMessage.builder()
            .id("projection-1")
            .ownerId(Accounts.DEFAULT_OWNER_ID)
            .transactionId("does-not-matter")
            .accountId("does-not-matter")
            .type(TransactionType.CREDIT)
            .amount(500L)
            .target(ProjectionTarget.INVOICE)
            .build();

    StepVerifier.create(listener.handle(message)).verifyComplete();
  }
}
