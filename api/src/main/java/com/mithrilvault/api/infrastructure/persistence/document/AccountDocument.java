package com.mithrilvault.api.infrastructure.persistence.document;

import com.mithrilvault.api.domain.model.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Document(collection = "accounts")
public class AccountDocument extends BaseDocument {
  private String ownerId;
  private String name;
  private AccountType type;
  private String institution;
  private Long initialBalance;
  private String color;
  private Boolean isActive;

  @Version private Long version;
}
