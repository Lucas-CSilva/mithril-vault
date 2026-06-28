package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.model.RefreshToken;
import com.mithrilvault.api.infrastructure.persistence.document.RefreshTokenDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

  RefreshToken toDomain(RefreshTokenDocument doc);

  @Mapping(target = "updatedAt", ignore = true)
  RefreshTokenDocument toDocument(RefreshToken token);
}
