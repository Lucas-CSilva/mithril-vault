package com.mithrilvault.api.domain.service;

import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.service.validation.AccountXorCardValidationRule;
import com.mithrilvault.api.domain.service.validation.RecurringEndDateValidationRule;
import com.mithrilvault.api.fixture.command.transaction.CreateTransactionCommands;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class TransactionValidationServiceTest {

  private final TransactionValidationService service =
      new TransactionValidationService(
          List.of(new AccountXorCardValidationRule(), new RecurringEndDateValidationRule()));

  @Test
  void completesEmpty_whenOnlyAccountIdIsSet() {
    var command = CreateTransactionCommands.validForAccount("account-1");

    // Regression guard: CreateTransactionCommandHandler chains this with `.then(...)`, which
    // requires the success path to actually complete (not hang or error) for the chain to
    // proceed to origin resolution.
    StepVerifier.create(service.validate(command)).verifyComplete();
  }

  @Test
  void errors_whenBothAccountIdAndCardIdAreSet() {
    var command = CreateTransactionCommands.withBothAccountAndCard("account-1", "card-1");

    StepVerifier.create(service.validate(command))
        .expectErrorMatches(
            ex ->
                ex instanceof BusinessException
                    && ex.getMessage().contains("Exactly one of accountId or cardId"))
        .verify();
  }

  @Test
  void errors_whenRecurringEndDateIsBeforeDate() {
    var command = CreateTransactionCommands.recurringWithEndDateBeforeDate("account-1");

    StepVerifier.create(service.validate(command))
        .expectErrorMatches(
            ex ->
                ex instanceof BusinessException
                    && ex.getMessage().contains("endDate must not be before date"))
        .verify();
  }

  @Test
  void ignoresRecurringOnlyRules_forSingleModeCommand() {
    // Regression guard: RecurringEndDateValidationRule.appliesTo() gates on mode == RECURRING,
    // so it must not run (and must not NPE on the null recurring config) for a SINGLE command.
    var command = CreateTransactionCommands.validForAccount("account-1");

    StepVerifier.create(service.validate(command)).verifyComplete();
  }
}
