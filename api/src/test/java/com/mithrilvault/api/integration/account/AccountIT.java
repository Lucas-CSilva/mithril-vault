package com.mithrilvault.api.integration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.mithrilvault.api.application.response.AccountResponse;
import com.mithrilvault.api.application.response.BalanceHistoryResponse;
import com.mithrilvault.api.config.AbstractIntegrationTest;
import com.mithrilvault.api.domain.command.account.ReconcileAccountCommand;
import com.mithrilvault.api.domain.command.account.UpdateAccountCommand;
import com.mithrilvault.api.domain.model.ReconciliationMethod;
import com.mithrilvault.api.fixture.command.account.CreateAccountCommands;
import com.mithrilvault.api.fixture.command.account.ReconcileAccountCommands;
import com.mithrilvault.api.infrastructure.persistence.AccountMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.RefreshTokenMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.UserMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AccountIT extends AbstractIntegrationTest {

  @Autowired private AccountMongoRepository accountMongoRepository;
  @Autowired private UserMongoRepository userMongoRepository;
  @Autowired private RefreshTokenMongoRepository refreshTokenMongoRepository;

  @BeforeEach
  void setUp() {
    accountMongoRepository.deleteAll().block();
    userMongoRepository.deleteAll().block();
    refreshTokenMongoRepository.deleteAll().block();

    String accessToken = userSteps.createAndGetAccessToken();
    accountSteps.setAccessToken(accessToken);
  }

  // ── GET /accounts ─────────────────────────────────────────────────────

  @Test
  void listAccounts_returns200_withOnlyActiveAccountsByDefault() {
    AccountResponse active = accountSteps.createAndGet(CreateAccountCommands.valid());
    String inactiveId = accountSteps.createAndGet(CreateAccountCommands.withName("Old Bank")).id();
    accountSteps.deactivate(inactiveId).expectStatus().isNoContent();

    accountSteps
        .list(false)
        .expectStatus()
        .isOk()
        .expectBodyList(AccountResponse.class)
        .value(
            list -> assertThat(list).extracting(AccountResponse::id).containsExactly(active.id()));
  }

  @Test
  void listAccounts_includesInactive_whenFlagIsSet() {
    AccountResponse active = accountSteps.createAndGet(CreateAccountCommands.valid());
    String inactiveId = accountSteps.createAndGet(CreateAccountCommands.withName("Old Bank")).id();
    accountSteps.deactivate(inactiveId).expectStatus().isNoContent();

    accountSteps
        .list(true)
        .expectStatus()
        .isOk()
        .expectBodyList(AccountResponse.class)
        .value(
            list ->
                assertThat(list)
                    .extracting(AccountResponse::id)
                    .containsExactlyInAnyOrder(active.id(), inactiveId));
  }

  @Test
  void listAccounts_returns401_whenUnauthenticated() {
    webTestClient.get().uri("/mithril-vault/accounts").exchange().expectStatus().isUnauthorized();
  }

  // ── POST /accounts ────────────────────────────────────────────────────

  @Test
  void createAccount_returns201_forValidAccount() {
    accountSteps
        .create(CreateAccountCommands.valid())
        .expectStatus()
        .isCreated()
        .expectBody(AccountResponse.class)
        .value(
            body -> {
              assertThat(body.id()).isNotNull();
              assertThat(body.name()).isEqualTo(CreateAccountCommands.DEFAULT_NAME);
              assertThat(body.isActive()).isTrue();
              assertThat(body.createdAt()).isNotNull();
              assertThat(body.currentBalance())
                  .isEqualTo(CreateAccountCommands.DEFAULT_INITIAL_BALANCE);
            });
  }

  @Test
  void createAccount_returns409_whenNameAlreadyUsedByOwner() {
    accountSteps.createAndGet(CreateAccountCommands.valid());

    accountSteps.create(CreateAccountCommands.valid()).expectStatus().isEqualTo(409);
  }

  @Test
  void createAccount_returns422_whenNameIsBlank() {
    accountSteps.create(CreateAccountCommands.withName("")).expectStatus().isEqualTo(422);
  }

  @Test
  void createAccount_returns401_whenUnauthenticated() {
    webTestClient
        .post()
        .uri("/mithril-vault/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAccountCommands.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  // ── GET /accounts/{id} ────────────────────────────────────────────────

  @Test
  void getAccount_returns200_forOwnedAccount() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());

    accountSteps
        .get(created.id())
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .value(body -> assertThat(body.id()).isEqualTo(created.id()));
  }

  @Test
  void getAccount_returns404_forOtherOwnersAccount() {
    AccountDocument otherDoc =
        accountMongoRepository
            .save(
                AccountDocument.builder()
                    .name("Other's Account")
                    .ownerId("completely-different-owner")
                    .initialBalance(1000L)
                    .isActive(true)
                    .build())
            .block();

    accountSteps.get(otherDoc.getId()).expectStatus().isNotFound();
  }

  @Test
  void getAccount_returns404_whenAccountDoesNotExist() {
    accountSteps.get("non-existent-id").expectStatus().isNotFound();
  }

  // ── PATCH /accounts/{id} ──────────────────────────────────────────────

  @Test
  void patchAccount_returns200_withUpdatedName() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());

    accountSteps
        .patch(created.id(), new UpdateAccountCommand("Bradesco Corrente", null, null, null))
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .value(body -> assertThat(body.name()).isEqualTo("Bradesco Corrente"));
  }

  @Test
  void patchAccount_returns404_forOtherOwnersAccount() {
    AccountDocument otherDoc =
        accountMongoRepository
            .save(
                AccountDocument.builder()
                    .name("Other's Account")
                    .ownerId("completely-different-owner")
                    .initialBalance(1000L)
                    .isActive(true)
                    .build())
            .block();

    accountSteps
        .patch(otherDoc.getId(), new UpdateAccountCommand("New Name", null, null, null))
        .expectStatus()
        .isNotFound();
  }

  // ── DELETE /accounts/{id} (deactivate) ────────────────────────────────

  @Test
  void deactivateAccount_returns204_andAccountIsHiddenFromDefaultList() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());

    accountSteps.deactivate(created.id()).expectStatus().isNoContent();

    accountSteps
        .list(false)
        .expectStatus()
        .isOk()
        .expectBodyList(AccountResponse.class)
        .value(
            list -> assertThat(list).extracting(AccountResponse::id).doesNotContain(created.id()));
  }

  @Test
  void deactivateAccount_preservesCreatedAt() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());
    assertThat(created.createdAt()).isNotNull();

    // MongoDB's BSON Date type only has millisecond precision, so the createdAt read back
    // right after creation (already round-tripped through Mongo) is the stable baseline —
    // comparing against it isolates "did deactivate null the field" from unrelated
    // nanosecond-vs-millisecond precision loss on the very first, pre-persistence response.
    AccountResponse persisted =
        accountSteps
            .get(created.id())
            .expectBody(AccountResponse.class)
            .returnResult()
            .getResponseBody();

    accountSteps.deactivate(created.id()).expectStatus().isNoContent();

    accountSteps
        .get(created.id())
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .value(body -> assertThat(body.createdAt()).isEqualTo(persisted.createdAt()));
  }

  @Test
  void deactivateAccount_returns404_forOtherOwnersAccount() {
    AccountDocument otherDoc =
        accountMongoRepository
            .save(
                AccountDocument.builder()
                    .name("Other's Account")
                    .ownerId("completely-different-owner")
                    .initialBalance(1000L)
                    .isActive(true)
                    .build())
            .block();

    accountSteps.deactivate(otherDoc.getId()).expectStatus().isNotFound();
  }

  // ── POST /accounts/{id}/reactivate ────────────────────────────────────

  @Test
  void reactivateAccount_returns200_andAccountIsVisibleAgain() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());
    accountSteps.deactivate(created.id()).expectStatus().isNoContent();

    accountSteps
        .reactivate(created.id())
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .value(body -> assertThat(body.isActive()).isTrue());

    accountSteps
        .list(false)
        .expectStatus()
        .isOk()
        .expectBodyList(AccountResponse.class)
        .value(list -> assertThat(list).extracting(AccountResponse::id).contains(created.id()));
  }

  @Test
  void reactivateAccount_returns404_whenAccountDoesNotExist() {
    accountSteps.reactivate("non-existent-id").expectStatus().isNotFound();
  }

  // ── POST /accounts/{id}/reconcile ─────────────────────────────────────

  @Test
  void reconcileAccount_adjustInitialBalance_returns200_withRealBalanceAsCurrentBalance() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());

    accountSteps
        .reconcile(created.id(), ReconcileAccountCommands.adjustInitialBalance())
        .expectStatus()
        .isOk()
        .expectBody(AccountResponse.class)
        .value(
            body -> {
              assertThat(body.initialBalance())
                  .isEqualTo(ReconcileAccountCommands.DEFAULT_REAL_BALANCE);
              assertThat(body.currentBalance())
                  .isEqualTo(ReconcileAccountCommands.DEFAULT_REAL_BALANCE);
            });
  }

  @Test
  void reconcileAccount_adjustingTransaction_returns422_notYetSupported() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());

    accountSteps
        .reconcile(
            created.id(),
            new ReconcileAccountCommand(148500L, ReconciliationMethod.ADJUSTING_TRANSACTION))
        .expectStatus()
        .isEqualTo(422);
  }

  @Test
  void reconcileAccount_returns404_forOtherOwnersAccount() {
    AccountDocument otherDoc =
        accountMongoRepository
            .save(
                AccountDocument.builder()
                    .name("Other's Account")
                    .ownerId("completely-different-owner")
                    .initialBalance(1000L)
                    .isActive(true)
                    .build())
            .block();

    accountSteps
        .reconcile(otherDoc.getId(), ReconcileAccountCommands.adjustInitialBalance())
        .expectStatus()
        .isNotFound();
  }

  // ── GET /accounts/{id}/balance-history ────────────────────────────────

  @Test
  void balanceHistory_returns200_with30Points() {
    AccountResponse created = accountSteps.createAndGet(CreateAccountCommands.valid());

    accountSteps
        .balanceHistory(created.id())
        .expectStatus()
        .isOk()
        .expectBody(BalanceHistoryResponse.class)
        .value(
            body -> {
              assertThat(body.accountId()).isEqualTo(created.id());
              assertThat(body.points()).hasSize(30);
            });
  }

  @Test
  void balanceHistory_returns404_whenAccountDoesNotExist() {
    accountSteps.balanceHistory("non-existent-id").expectStatus().isNotFound();
  }
}
