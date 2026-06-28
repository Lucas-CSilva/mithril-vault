package com.mithrilvault.api.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Document(collection = "categories")
public class CategoryDocument extends BaseDocument {
  private String name;
  private String parentId;
  private String icon;
  private String color;
  private String ownerId;
  private boolean isSystem;
}
