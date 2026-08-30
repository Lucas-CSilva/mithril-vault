package com.mithrilvault.api.domain.commandhandler.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.exception.BusinessException;
import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionFrequency;
import com.mithrilvault.api.domain.model.TransactionOrigin;
import com.mithrilvault.api.domain.port.RecurringSeriesRepository;
import com.mithrilvault.api.domain.service.TransactionOriginResolver;
import com.mithrilvault.api.domain.service.TransactionValidationService;
import com.mithrilvault.api.domain.service.validation.AccountXorCardValidationRule;
import com.mithrilvault.api.domain.service.validation.RecurringEndDateValidationRule;
import com.mithrilvault.api.fixture.command.transaction.CreateTransactionCommands;
import com.mithrilvault.api.fixture.model.Accounts;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateRecurringTransactionCommandHandlerTest {

  @Mock private TransactionOriginResolver originResolver;
  @Mock private RecurringSeriesRepository recurringSeriesRepository;

  private CreateRecurringTransactionCommandHandler handler;

  @BeforeEach
  void setUp() {
    var clock = Clock.system(ZoneId.of("America/Sao_Paulo"));
    handler =
        new CreateRecurringTransactionCommandHandler(
            clock,
            new TransactionValidationService(
                List.of(new AccountXorCardValidationRule(), new RecurringEndDateValidationRule())),
            originResolver,
            recurringSeriesRepository);
  }

  @Test
  void insertsDueInstance_andAdvancesNextOccurrence_whenDateIsDueOrPast() {
    var account = Accounts.checking();
    var dueDate = LocalDate.now().minusDays(1);
    var command = CreateTransactionCommands.recurring(account.id(), dueDate, null);
    var savedTransaction = Transaction.builder().id("txn-1").accountId(account.id()).build();

    when(originResolver.resolve(command, Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(new TransactionOrigin(account, null, null)));
    when(recurringSeriesRepository.saveWithInstance(any(), any()))
        .thenReturn(Flux.just(savedTransaction));

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .expectNext(savedTransaction)
        .verifyComplete();

    ArgumentCaptor<RecurringTransactionSeries> seriesCaptor =
        ArgumentCaptor.forClass(RecurringTransactionSeries.class);
    verify(recurringSeriesRepository).saveWithInstance(seriesCaptor.capture(), any());
    assertThat(seriesCaptor.getValue().nextOccurrenceDate())
        .isEqualTo(TransactionFrequency.MONTHLY.advance(dueDate));
    assertThat(seriesCaptor.getValue().recurringSeriesId()).isNotBlank();
    verify(recurringSeriesRepository, never()).save(any());
  }

  @Test
  void onlySavesSeries_whenDateIsInTheFuture() {
    var account = Accounts.checking();
    var futureDate = LocalDate.now().plusMonths(2);
    var command = CreateTransactionCommands.recurring(account.id(), futureDate, null);

    when(originResolver.resolve(command, Accounts.DEFAULT_OWNER_ID))
        .thenReturn(Mono.just(new TransactionOrigin(account, null, null)));
    when(recurringSeriesRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID)).verifyComplete();

    ArgumentCaptor<RecurringTransactionSeries> seriesCaptor =
        ArgumentCaptor.forClass(RecurringTransactionSeries.class);
    verify(recurringSeriesRepository).save(seriesCaptor.capture());
    assertThat(seriesCaptor.getValue().nextOccurrenceDate()).isEqualTo(futureDate);
    verify(recurringSeriesRepository, never()).saveWithInstance(any(), any());
  }

  @Test
  void doesNotResolveOriginOrSave_whenEndDateIsBeforeDate() {
    var command = CreateTransactionCommands.recurringWithEndDateBeforeDate("account-1");

    StepVerifier.create(handler.handle(command, Accounts.DEFAULT_OWNER_ID))
        .expectError(BusinessException.class)
        .verify();

    verify(originResolver, never()).resolve(any(), any());
    verify(recurringSeriesRepository, never()).save(any());
    verify(recurringSeriesRepository, never()).saveWithInstance(any(), any());
  }
}
