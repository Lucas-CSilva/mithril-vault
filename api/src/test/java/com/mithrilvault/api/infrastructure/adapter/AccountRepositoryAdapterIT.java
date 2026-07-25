package com.mithrilvault.api.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.domain.model.BalancePoint;
import com.mithrilvault.api.fixture.model.Accounts;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import java.time.Instant;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.test.StepVerifier;

class AccountRepositoryAdapterIT extends AbstractIntegrationTest {

  @Autowired private AccountRepositoryAdapter adapter;
  @Autowired private AccountMongoRepository accountMongoRepository;
  @Autowired private ReactiveMongoTemplate reactiveMongoTemplate;

  @BeforeEach
  void cleanUp() {
    accountMongoRepository.deleteAll().block();
    reactiveMongoTemplate.remove(new Document(), "transactions").block();
  }

  // ── save ──────────────────────────────────────────────────────────────

  @Test
  void save_persistsAccount_andReturnsDomainModel() {
    Account toSave = Accounts.checking().toBuilder().id(null).build();

    StepVerifier.create(adapter.save(toSave))
        .assertNext(
            saved -> {
              assertThat(saved.id()).isNotNull();
              assertThat(saved.name()).isEqualTo(Accounts.checking().name());
              assertThat(saved.ownerId()).isEqualTo(Accounts.DEFAULT_OWNER_ID);
            })
        .verifyComplete();
  }

  @Test
  void save_throwsConflictException_whenNameDuplicatedForSameOwner() {
    Account first = Accounts.checking().toBuilder().id(null).build();
    adapter.save(first).block();

    Account duplicate = Accounts.checking().toBuilder().id(null).build();

    StepVerifier.create(adapter.save(duplicate)).expectError(ConflictException.class).verify();
  }

  @Test
  void save_populatesCreatedAt_onInsert() {
    Account toSave = Accounts.checking().toBuilder().id(null).createdAt(null).build();

    StepVerifier.create(adapter.save(toSave))
        .assertNext(saved -> assertThat(saved.createdAt()).isNotNull())
        .verifyComplete();
  }

  @Test
  void save_preservesCreatedAt_onUpdate() {
    Account inserted = adapter.save(Accounts.checking().toBuilder().id(null).build()).block();
    // MongoDB's BSON Date type only has millisecond precision, so re-read the persisted value
    // as the baseline rather than the JVM-precision Instant returned by the insert itself —
    // otherwise the comparison would fail on precision loss unrelated to the fix under test.
    Account saved = adapter.findByIdAndOwnerId(inserted.id(), inserted.ownerId()).block();
    Instant originalCreatedAt = saved.createdAt();
    assertThat(originalCreatedAt).isNotNull();

    Account deactivated = saved.deactivate();

    StepVerifier.create(adapter.save(deactivated))
        .assertNext(
            updated -> {
              assertThat(updated.isActive()).isFalse();
              assertThat(updated.createdAt()).isEqualTo(originalCreatedAt);
            })
        .verifyComplete();

    StepVerifier.create(accountMongoRepository.findById(saved.id()))
        .assertNext(doc -> assertThat(doc.getCreatedAt()).isEqualTo(originalCreatedAt))
        .verifyComplete();
  }

  // ── findByIdAndOwnerId ────────────────────────────────────────────────

  @Test
  void findByIdAndOwnerId_returnsEmpty_forOtherOwnersAccount() {
    AccountDocument doc =
        accountMongoRepository
            .save(
                AccountDocument.builder()
                    .name("Other's Account")
                    .ownerId(Accounts.OTHER_OWNER_ID)
                    .initialBalance(1000L)
                    .isActive(true)
                    .build())
            .block();

    StepVerifier.create(adapter.findByIdAndOwnerId(doc.getId(), Accounts.DEFAULT_OWNER_ID))
        .verifyComplete();
  }

  // ── findAllByOwnerId ──────────────────────────────────────────────────

  @Test
  void findAllByOwnerId_excludesInactive_whenNotIncluded() {
    accountMongoRepository
        .save(
            AccountDocument.builder()
                .name("Active")
                .ownerId(Accounts.DEFAULT_OWNER_ID)
                .initialBalance(1000L)
                .isActive(true)
                .build())
        .block();
    accountMongoRepository
        .save(
            AccountDocument.builder()
                .name("Inactive")
                .ownerId(Accounts.DEFAULT_OWNER_ID)
                .initialBalance(1000L)
                .isActive(false)
                .build())
        .block();

    StepVerifier.create(adapter.findAllByOwnerId(Accounts.DEFAULT_OWNER_ID, false).collectList())
        .assertNext(
            list -> {
              assertThat(list).hasSize(1);
              assertThat(list.get(0).name()).isEqualTo("Active");
            })
        .verifyComplete();
  }

  // ── currentBalance ────────────────────────────────────────────────────

  @Test
  void currentBalance_equalsInitialBalance_whenNoTransactionsExist() {
    StepVerifier.create(adapter.currentBalance("account-1", Accounts.DEFAULT_OWNER_ID, 150000L))
        .assertNext(balance -> assertThat(balance).isEqualTo(150000L))
        .verifyComplete();
  }

  @Test
  void currentBalance_addsCreditsAndSubtractsDebits() {
    reactiveMongoTemplate
        .save(
            new Document(
                Map.of(
                    "ownerId",
                    Accounts.DEFAULT_OWNER_ID,
                    "accountId",
                    "account-1",
                    "type",
                    "CREDIT",
                    "amount",
                    5_000L)),
            "transactions")
        .block();
    reactiveMongoTemplate
        .save(
            new Document(
                Map.of(
                    "ownerId",
                    Accounts.DEFAULT_OWNER_ID,
                    "accountId",
                    "account-1",
                    "type",
                    "DEBIT",
                    "amount",
                    2_000L)),
            "transactions")
        .block();

    StepVerifier.create(adapter.currentBalance("account-1", Accounts.DEFAULT_OWNER_ID, 100_000L))
        .assertNext(balance -> assertThat(balance).isEqualTo(103_000L))
        .verifyComplete();
  }

  // ── balanceHistory ────────────────────────────────────────────────────

  @Test
  void balanceHistory_returnsThirtyAscendingDailyPoints() {
    StepVerifier.create(
            adapter
                .balanceHistory("account-1", Accounts.DEFAULT_OWNER_ID, 150000L, 30)
                .collectList())
        .assertNext(
            points -> {
              assertThat(points).hasSize(30);
              assertThat(points).extracting(BalancePoint::balance).containsOnly(150000L);
              assertThat(points)
                  .isSortedAccordingTo(java.util.Comparator.comparing(BalancePoint::date));
            })
        .verifyComplete();
  }
}
