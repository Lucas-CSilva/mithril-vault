package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import com.mithrilvault.api.domain.model.Transaction;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecurringSeriesRepository {
  Mono<RecurringTransactionSeries> save(RecurringTransactionSeries recurringSeries);

  Mono<Void> advance(String seriesId, LocalDate nextOccurrenceDate, Long expectedVersion);

  Flux<Transaction> saveWithInstance(
      RecurringTransactionSeries recurringSeries, Transaction transaction);
}
