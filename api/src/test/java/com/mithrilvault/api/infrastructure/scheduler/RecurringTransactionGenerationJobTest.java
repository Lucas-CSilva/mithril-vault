package com.mithrilvault.api.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.domain.command.transaction.CreateTransactionCommand;
import com.mithrilvault.api.domain.commandhandler.transaction.CreateTransactionCommandHandler;
import com.mithrilvault.api.domain.config.AppProperties;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.model.TransactionFrequency;
import com.mithrilvault.api.domain.model.TransactionType;
import com.mithrilvault.api.domain.port.RecurringSeriesReadRepository;
import com.mithrilvault.api.domain.port.RecurringSeriesRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionGenerationJobTest {

  @Mock private RecurringSeriesRepository recurringSeriesRepository;
  @Mock private RecurringSeriesReadRepository recurringSeriesReadRepository;
  @Mock private CreateTransactionCommandHandler createTransactionCommandHandler;

  private MeterRegistry meterRegistry;
  private RecurringTransactionGenerationJob job;

  @BeforeEach
  void setUp() {
    var schedulerConfig =
        new AppProperties.SchedulerConfig(
            new AppProperties.SchedulerJobConfig(
                CronExpression.parse("0 0 0 1 * *"), 16, 3, Duration.ofMillis(200)),
            new AppProperties.SchedulerJobConfig(
                CronExpression.parse("0 0 0 * * *"), 16, 3, Duration.ofMillis(200)),
            new AppProperties.SchedulerJobConfig(
                CronExpression.parse("0 0 1 * * *"), 16, 3, Duration.ofMillis(200)),
            ZoneId.of("America/Sao_Paulo"));
    var appProperties =
        new AppProperties(null, null, null, null, schedulerConfig, ZoneId.of("America/Sao_Paulo"));

    meterRegistry = new SimpleMeterRegistry();
    job =
        new RecurringTransactionGenerationJob(
            appProperties,
            recurringSeriesRepository,
            recurringSeriesReadRepository,
            createTransactionCommandHandler,
            new SchedulerJobMetrics(meterRegistry));
  }

  private static RecurringTransactionSeries series(String id, LocalDate nextOccurrenceDate) {
    return RecurringTransactionSeries.builder()
        .id(id)
        .ownerId("owner-" + id)
        .recurringSeriesId("recurring-" + id)
        .frequency(TransactionFrequency.MONTHLY)
        .nextOccurrenceDate(nextOccurrenceDate)
        .type(TransactionType.DEBIT)
        .amount(1_000L)
        .description("Subscription")
        .paymentMethod(PaymentMethod.PIX)
        .accountId("account-" + id)
        .tags(Set.of())
        .version(0L)
        .build();
  }

  private static Transaction generatedTransaction(RecurringTransactionSeries series) {
    return Transaction.builder().id("txn-" + series.id()).accountId(series.accountId()).build();
  }

  private double outcomeTotal(String outcome) {
    var counter =
        meterRegistry
            .find("scheduler.job.outcome.total")
            .tag("job", "recurringGeneration")
            .tag("outcome", outcome)
            .counter();
    return counter == null ? 0 : counter.count();
  }

  @Test
  void generatesInstance_andAdvancesSeries_whenSeriesIsDue() {
    var series = series("series-1", LocalDate.now().minusDays(1));

    when(recurringSeriesReadRepository.findDueSeries(any())).thenReturn(Flux.just(series));
    when(createTransactionCommandHandler.handle(
            any(CreateTransactionCommand.class),
            eq(series.ownerId()),
            eq(series.recurringSeriesId())))
        .thenReturn(Flux.just(generatedTransaction(series)));
    when(recurringSeriesRepository.advance(
            series.id(), series.nextOccurrenceDate(), series.version()))
        .thenReturn(Mono.empty());

    StepVerifier.create(job.execute()).verifyComplete();

    verify(recurringSeriesRepository)
        .advance(series.id(), series.nextOccurrenceDate(), series.version());
    assertThat(outcomeTotal("generated")).isEqualTo(1);
    assertThat(outcomeTotal("conflict")).isEqualTo(0);
    assertThat(outcomeTotal("failed")).isEqualTo(0);
  }

  @Test
  void noOp_whenNoDueSeries() {
    when(recurringSeriesReadRepository.findDueSeries(any())).thenReturn(Flux.empty());

    StepVerifier.create(job.execute()).verifyComplete();

    verify(createTransactionCommandHandler, never()).handle(any(), any(), any());
    verify(recurringSeriesRepository, never()).advance(any(), any(), any());
    assertThat(outcomeTotal("generated")).isEqualTo(0);
  }

  @Test
  void continuesRemainingSeries_whenOneSeriesFailsWithNonConflictError() {
    var failingSeries = series("series-1", LocalDate.now().minusDays(1));
    var healthySeries = series("series-2", LocalDate.now().minusDays(1));

    when(recurringSeriesReadRepository.findDueSeries(any()))
        .thenReturn(Flux.just(failingSeries, healthySeries));
    when(createTransactionCommandHandler.handle(
            any(CreateTransactionCommand.class),
            eq(failingSeries.ownerId()),
            eq(failingSeries.recurringSeriesId())))
        .thenReturn(Flux.error(new RuntimeException("boom")));
    when(createTransactionCommandHandler.handle(
            any(CreateTransactionCommand.class),
            eq(healthySeries.ownerId()),
            eq(healthySeries.recurringSeriesId())))
        .thenReturn(Flux.just(generatedTransaction(healthySeries)));
    when(recurringSeriesRepository.advance(
            healthySeries.id(), healthySeries.nextOccurrenceDate(), healthySeries.version()))
        .thenReturn(Mono.empty());

    StepVerifier.create(job.execute()).verifyComplete();

    verify(recurringSeriesRepository, never()).advance(eq(failingSeries.id()), any(), any());
    verify(recurringSeriesRepository)
        .advance(healthySeries.id(), healthySeries.nextOccurrenceDate(), healthySeries.version());
    assertThat(outcomeTotal("failed")).isEqualTo(1);
    assertThat(outcomeTotal("generated")).isEqualTo(1);
  }

  @Test
  void healsWithinSameRun_whenConflictResolvesOnRetry() {
    var series = series("series-1", LocalDate.now().minusDays(1));

    when(recurringSeriesReadRepository.findDueSeries(any())).thenReturn(Flux.just(series));
    when(createTransactionCommandHandler.handle(
            any(CreateTransactionCommand.class),
            eq(series.ownerId()),
            eq(series.recurringSeriesId())))
        .thenReturn(Flux.just(generatedTransaction(series)));
    var attempt = new AtomicInteger();
    when(recurringSeriesRepository.advance(
            series.id(), series.nextOccurrenceDate(), series.version()))
        .thenReturn(
            Mono.defer(
                () ->
                    attempt.incrementAndGet() == 1
                        ? Mono.error(new ConflictException("version conflict"))
                        : Mono.empty()));

    StepVerifier.create(job.execute()).verifyComplete();

    assertThat(attempt.get()).isEqualTo(2);
    assertThat(outcomeTotal("conflict")).isEqualTo(1);
    assertThat(outcomeTotal("generated")).isEqualTo(1);
    assertThat(outcomeTotal("failed")).isEqualTo(0);
  }

  @Test
  void givesUp_afterExhaustingConflictRetries() {
    var series = series("series-1", LocalDate.now().minusDays(1));

    when(recurringSeriesReadRepository.findDueSeries(any())).thenReturn(Flux.just(series));
    when(createTransactionCommandHandler.handle(
            any(CreateTransactionCommand.class),
            eq(series.ownerId()),
            eq(series.recurringSeriesId())))
        .thenReturn(Flux.just(generatedTransaction(series)));
    when(recurringSeriesRepository.advance(
            series.id(), series.nextOccurrenceDate(), series.version()))
        .thenReturn(Mono.error(new ConflictException("version conflict")));

    StepVerifier.create(job.execute()).verifyComplete();

    // 1 initial attempt + 3 retries (conflictRetryMaxAttempts from test config) = 4 lost races,
    // even though recurringSeriesRepository.advance(...) is only invoked once: the retries
    // resubscribe to the same (cold) Mono it returned rather than calling it again.
    assertThat(outcomeTotal("conflict")).isEqualTo(4);
    assertThat(outcomeTotal("generated")).isEqualTo(0);
    assertThat(outcomeTotal("failed")).isEqualTo(1);
  }
}
