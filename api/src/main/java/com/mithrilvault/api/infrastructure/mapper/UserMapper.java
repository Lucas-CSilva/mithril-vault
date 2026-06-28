package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.model.User;
import com.mithrilvault.api.infrastructure.persistence.document.UserDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  User toDomain(UserDocument doc);

  @Mapping(target = "updatedAt", ignore = true)
  UserDocument toDocument(User user);
}
