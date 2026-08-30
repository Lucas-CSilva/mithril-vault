package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.config.MapperBaseConfig;
import com.mithrilvault.api.domain.model.RecurringTransactionSeries;
import com.mithrilvault.api.infrastructure.persistence.document.RecurringTransactionSeriesDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperBaseConfig.class)
public interface RecurringTransactionSeriesMapper {
  @Mapping(target = "updatedAt", ignore = true)
  RecurringTransactionSeriesDocument toDocument(RecurringTransactionSeries transaction);

  RecurringTransactionSeries toDomain(RecurringTransactionSeriesDocument transactionDocument);
}
