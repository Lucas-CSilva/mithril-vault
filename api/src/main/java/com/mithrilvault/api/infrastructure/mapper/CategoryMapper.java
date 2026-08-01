package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.config.MapperBaseConfig;
import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperBaseConfig.class)
public interface CategoryMapper {

  @Mapping(target = "isSystem", source = "system")
  Category toDomain(CategoryDocument document);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  CategoryDocument toDocument(Category category);
}
