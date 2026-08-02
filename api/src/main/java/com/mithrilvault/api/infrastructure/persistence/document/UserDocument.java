package com.mithrilvault.api.infrastructure.persistence.document;

import com.mithrilvault.api.domain.model.UserStatus;
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
@Document(collection = "users")
public class UserDocument extends BaseDocument {

  private String email;
  private String passwordHash;
  private String displayName;
  private UserStatus status;
}
