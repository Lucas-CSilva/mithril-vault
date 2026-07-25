package com.mithrilvault.api.infrastructure.adapter;

import com.mithrilvault.api.domain.config.SystemCategoryIds;
import org.springframework.stereotype.Component;

@Component
public class SystemCategoryIdsImpl implements SystemCategoryIds {

  static final String OUTROS_ID = "system-category-outros-00000000001";

  @Override
  public String getOutrosId() {
    return OUTROS_ID;
  }
}
