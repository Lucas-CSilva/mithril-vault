package com.mithrilvault.api.application.mapper;

import com.mithrilvault.api.application.response.CategoryResponse;
import com.mithrilvault.api.domain.config.MapperConfig;
import com.mithrilvault.api.domain.model.Category;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface CategoryResponseMapper {

  CategoryResponse toResponse(Category category);
}
