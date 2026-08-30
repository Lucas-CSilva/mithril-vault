package com.mithrilvault.api.domain.port;

import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import java.time.LocalDate;
import reactor.core.publisher.Flux;

public interface RecurringSeriesReadRepository {
  Flux<RecurringTransactionSeries> findDueSeries(LocalDate asOfDate);
}
