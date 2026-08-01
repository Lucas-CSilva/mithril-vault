package com.mithrilvault.api.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.fixture.model.Accounts;
import com.mithrilvault.api.infrastructure.mapper.TransactionMapper;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.TransactionMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class ProjectionRepositoryAdapterIT extends AbstractIntegrationTest {

  @Autowired private ProjectionRepositoryAdapter adapter;
  @Autowired private AccountMongoRepository accountMongoRepository;
  @Autowired private TransactionMongoRepository transactionMongoRepository;
  @Autowired private TransactionMapper transactionMapper;
  @Autowired private MeterRegistry meterRegistry;

  @BeforeEach
  void cleanUp() {
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

  private TransactionDocument seedTransaction(Set<String> appliedProjections) {
    return transactionMongoRepository
        .save(
            TransactionDocument.builder()
                .ownerId(Accounts.DEFAULT_OWNER_ID)
                .type(TransactionType.CREDIT)
                .amount(500L)
                .date(LocalDate.parse("2026-01-01"))
                .accountId("account-1")
                .appliedProjections(appliedProjections)
                .build())
        .block();
  }

  private TransactionDocument withProjectionApplied(
      TransactionDocument transaction, String projectionId) {
    return TransactionDocument.builder()
        .id(transaction.getId())
        .ownerId(transaction.getOwnerId())
        .type(transaction.getType())
        .amount(transaction.getAmount())
        .date(transaction.getDate())
        .accountId(transaction.getAccountId())
        .appliedProjections(Set.of(projectionId))
        .build();
  }

  @Test
  void marksProjectionApplied_andIncrementsAccountBalance() {
    AccountDocument account = seedAccount();
    TransactionDocument transaction = seedTransaction(Set.of());
    var updatedTransaction = withProjectionApplied(transaction, "projection-1");

    StepVerifier.create(
            adapter.markAppliedAndUpdateBalance(
                "projection-1",
                Accounts.DEFAULT_OWNER_ID,
                account.getId(),
                500L,
                transactionMapper.toDomain(updatedTransaction)))
        .verifyComplete();

    AccountDocument reloaded = accountMongoRepository.findById(account.getId()).block();
    assertThat(reloaded.getCurrentBalance()).isEqualTo(1_500L);
    assertThat(reloaded.getVersion()).isEqualTo(account.getVersion() + 1);

    TransactionDocument reloadedTransaction =
        transactionMongoRepository.findById(transaction.getId()).block();
    assertThat(reloadedTransaction.getAppliedProjections()).contains("projection-1");
  }

  @Test
  void isIdempotent_whenTheSameProjectionIsAppliedTwice() {
    AccountDocument account = seedAccount();
    TransactionDocument transaction = seedTransaction(Set.of());
    var updatedTransaction = withProjectionApplied(transaction, "projection-1");

    adapter
        .markAppliedAndUpdateBalance(
            "projection-1",
            Accounts.DEFAULT_OWNER_ID,
            account.getId(),
            500L,
            transactionMapper.toDomain(updatedTransaction))
        .block();

    double noopsBefore = replayNoopCount();

    // Simulates a redelivered message: the caller (handler) re-fetches the transaction, which
    // now already carries "projection-1" in its appliedProjections, and calls this method again
    // with the same projectionId — the DB-level filter must reject it as a no-op.
    StepVerifier.create(
            adapter.markAppliedAndUpdateBalance(
                "projection-1",
                Accounts.DEFAULT_OWNER_ID,
                account.getId(),
                500L,
                transactionMapper.toDomain(updatedTransaction)))
        .verifyComplete();

    AccountDocument reloaded = accountMongoRepository.findById(account.getId()).block();
    assertThat(reloaded.getCurrentBalance()).isEqualTo(1_500L);
    assertThat(replayNoopCount()).isEqualTo(noopsBefore + 1);
  }

  @Test
  void rollsBackBothWrites_whenTheAccountDoesNotExist() {
    TransactionDocument transaction = seedTransaction(Set.of());
    var updatedTransaction = withProjectionApplied(transaction, "projection-1");

    StepVerifier.create(
            adapter.markAppliedAndUpdateBalance(
                "projection-1",
                Accounts.DEFAULT_OWNER_ID,
                "does-not-exist",
                500L,
                transactionMapper.toDomain(updatedTransaction)))
        .expectError(NotFoundException.class)
        .verify();

    TransactionDocument reloadedTransaction =
        transactionMongoRepository.findById(transaction.getId()).block();
    assertThat(reloadedTransaction.getAppliedProjections()).doesNotContain("projection-1");
  }

  @Test
  void rollsBackBothWrites_whenTheAccountBelongsToAnotherOwner() {
    AccountDocument account = seedAccount();
    TransactionDocument transaction = seedTransaction(Set.of());
    var updatedTransaction = withProjectionApplied(transaction, "projection-1");

    StepVerifier.create(
            adapter.markAppliedAndUpdateBalance(
                "projection-1",
                Accounts.OTHER_OWNER_ID,
                account.getId(),
                500L,
                transactionMapper.toDomain(updatedTransaction)))
        .expectError(NotFoundException.class)
        .verify();

    AccountDocument reloaded = accountMongoRepository.findById(account.getId()).block();
    assertThat(reloaded.getCurrentBalance()).isEqualTo(1_000L);

    TransactionDocument reloadedTransaction =
        transactionMongoRepository.findById(transaction.getId()).block();
    assertThat(reloadedTransaction.getAppliedProjections()).doesNotContain("projection-1");
  }

  private double replayNoopCount() {
    var counter = meterRegistry.find("projection.consumer.replay.noop.total").counter();
    return counter == null ? 0.0 : counter.count();
  }
}
