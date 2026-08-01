package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.config.MapperBaseConfig;
import com.mithrilvault.api.domain.model.Transaction;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import org.mapstruct.Mapper;

@Mapper(config = MapperBaseConfig.class)
public interface TransactionMapper {
  TransactionDocument toDocument(Transaction transaction);

  Transaction toDomain(TransactionDocument transactionDocument);
}
