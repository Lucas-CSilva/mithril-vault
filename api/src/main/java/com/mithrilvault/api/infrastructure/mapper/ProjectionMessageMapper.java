package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.command.account.ApplyAccountBalanceProjectionCommand;
import com.mithrilvault.api.domain.config.MapperBaseConfig;
import com.mithrilvault.api.infrastructure.adapter.messaging.BalanceProjectionMessage;
import com.mithrilvault.api.infrastructure.adapter.messaging.ProjectionTarget;
import com.mithrilvault.api.infrastructure.persistence.document.TransactionDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperBaseConfig.class)
public interface ProjectionMessageMapper {

  @Mapping(target = "id", expression = "java(target.projectionName())")
  @Mapping(target = "transactionId", source = "transactionDocument.id")
  BalanceProjectionMessage toBalanceProjectionMessage(
      TransactionDocument transactionDocument, ProjectionTarget target);

  @Mapping(target = "projectionId", source = "id")
  ApplyAccountBalanceProjectionCommand toAccountBalance(BalanceProjectionMessage message);
}
