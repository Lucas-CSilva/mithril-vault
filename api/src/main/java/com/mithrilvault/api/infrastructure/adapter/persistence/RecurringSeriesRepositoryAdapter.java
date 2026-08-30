package com.mithrilvault.api.infrastructure.adapter.persistence;

import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.domain.port.RecurringSeriesReadRepository;
import com.mithrilvault.api.domain.port.RecurringSeriesRepository;
import com.mithrilvault.api.domain.port.TransactionRepository;
import com.mithrilvault.api.infrastructure.mapper.RecurringTransactionSeriesMapper;
import com.mithrilvault.api.infrastructure.persistence.RecurringTransactionSeriesMongoRepository;
import com.mithrilvault.api.infrastructure.persistence.document.BaseDocument;
import com.mithrilvault.api.infrastructure.persistence.document.RecurringTransactionSeriesDocument;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RecurringSeriesRepositoryAdapter
    implements RecurringSeriesRepository, RecurringSeriesReadRepository {

  private final ReactiveMongoTemplate mongoTemplate;
  private final RecurringTransactionSeriesMapper mapper;
  private final TransactionalOperator transactionalOperator;
  private final RecurringTransactionSeriesMongoRepository mongoRepository;
  private final TransactionRepository transactionRepository;

  @Override
  public Flux<RecurringTransactionSeries> findDueSeries(LocalDate asOfDate) {
    var isDue =
        Criteria.where(RecurringTransactionSeriesDocument.Fields.nextOccurrenceDate).lte(asOfDate);
    var notPastEndDate =
        new Criteria()
            .orOperator(
                Criteria.where(RecurringTransactionSeriesDocument.Fields.endDate).isNull(),
                Criteria.where("$expr")
                    .is(
                        new Document(
                            "$gte",
                            List.of(
                                "$" + RecurringTransactionSeriesDocument.Fields.endDate,
                                "$"
                                    + RecurringTransactionSeriesDocument.Fields
                                        .nextOccurrenceDate))));

    return mongoTemplate
        .find(
            Query.query(new Criteria().andOperator(isDue, notPastEndDate)),
            RecurringTransactionSeriesDocument.class)
        .map(mapper::toDomain);
  }

  @Override
  public Mono<RecurringTransactionSeries> save(RecurringTransactionSeries recurringSeries) {
    return mongoRepository.save(mapper.toDocument(recurringSeries)).map(mapper::toDomain);
  }

  @Override
  public Mono<Void> advance(String seriesId, LocalDate nextOccurrenceDate, Long expectedVersion) {
    var query =
        Query.query(
            Criteria.where("_id")
                .is(seriesId)
                .and(BaseDocument.Fields.version)
                .is(expectedVersion));
    var update =
        new Update()
            .set(RecurringTransactionSeriesDocument.Fields.nextOccurrenceDate, nextOccurrenceDate)
            .inc(BaseDocument.Fields.version, 1);

    return mongoTemplate
        .updateFirst(query, update, RecurringTransactionSeriesDocument.class)
        .filter(result -> result.getMatchedCount() == 1 && result.getModifiedCount() == 1)
        .switchIfEmpty(
            Mono.error(
                () ->
                    new ConflictException(
                        "RecurringTransactionSeries " + seriesId + " was modified concurrently")))
        .then();
  }

  @Override
  public Flux<Transaction> saveWithInstance(
      RecurringTransactionSeries recurringSeries, Transaction transaction) {
    return transactionalOperator
        .execute(
            tx ->
                Mono.zip(
                    mongoRepository.save(mapper.toDocument(recurringSeries)),
                    transactionRepository.save(transaction)))
        .flatMap(result -> Mono.just(result.getT2()));
  }
}
