package com.mithrilvault.api.infrastructure.persistence;

import com.mithrilvault.api.infrastructure.persistence.document.RecurringTransactionSeriesDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface RecurringTransactionSeriesMongoRepository
    extends ReactiveMongoRepository<RecurringTransactionSeriesDocument, String> {}
