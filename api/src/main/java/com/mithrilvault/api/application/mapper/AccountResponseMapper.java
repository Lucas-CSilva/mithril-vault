package com.mithrilvault.api.application.mapper;

import com.mithrilvault.api.application.response.AccountResponse;
import com.mithrilvault.api.domain.config.MapperBaseConfig;
import com.mithrilvault.api.domain.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperBaseConfig.class)
public interface AccountResponseMapper {

  @Mapping(target = "currentBalance", source = "currentBalance")
  AccountResponse toResponse(Account account, Long currentBalance);
}
