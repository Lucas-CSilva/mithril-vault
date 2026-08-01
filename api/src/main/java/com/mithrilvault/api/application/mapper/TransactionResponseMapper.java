package com.mithrilvault.api.application.mapper;

import com.mithrilvault.api.application.response.TransactionResponse;
import com.mithrilvault.api.domain.config.MapperBaseConfig;
import com.mithrilvault.api.domain.model.Transaction;
import org.mapstruct.Mapper;

@Mapper(config = MapperBaseConfig.class)
public interface TransactionResponseMapper {

  TransactionResponse toResponse(Transaction transaction);
}
