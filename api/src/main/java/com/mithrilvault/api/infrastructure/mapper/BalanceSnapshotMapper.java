package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.config.MapperBaseConfig;
import com.mithrilvault.api.domain.model.BalanceSnapshot;
import com.mithrilvault.api.infrastructure.persistence.document.BalanceSnapshotDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperBaseConfig.class)
public interface BalanceSnapshotMapper {
  BalanceSnapshot toDomain(BalanceSnapshotDocument document);

  @Mapping(target = "updatedAt", ignore = true)
  BalanceSnapshotDocument toDocument(BalanceSnapshot account);
}
