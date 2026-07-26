package com.mithrilvault.api.application.mapper;

import com.mithrilvault.api.application.response.TransactionResponse;
import com.mithrilvault.api.domain.config.MapperConfig;
import com.mithrilvault.api.domain.model.Transaction;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface TransactionResponseMapper {

  TransactionResponse toResponse(Transaction transaction);
}
