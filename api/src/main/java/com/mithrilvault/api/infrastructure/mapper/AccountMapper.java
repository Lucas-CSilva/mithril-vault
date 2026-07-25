package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.config.MapperConfig;
import com.mithrilvault.api.domain.model.Account;
import com.mithrilvault.api.infrastructure.persistence.document.AccountDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface AccountMapper {
  Account toDomain(AccountDocument document);

  @Mapping(target = "updatedAt", ignore = true)
  AccountDocument toDocument(Account account);
}
