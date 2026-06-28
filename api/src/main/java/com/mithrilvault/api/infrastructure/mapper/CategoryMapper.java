package com.mithrilvault.api.infrastructure.mapper;

import com.mithrilvault.api.domain.model.Category;
import com.mithrilvault.api.infrastructure.persistence.document.CategoryDocument;
import org.mapstruct.Mapping;

public interface CategoryMapper {
  Category toDomain(CategoryDocument document);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  CategoryDocument toDocument(Category category);
}
