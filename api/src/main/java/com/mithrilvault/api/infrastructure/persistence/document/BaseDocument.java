package com.mithrilvault.api.infrastructure.persistence.document;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants
@AllArgsConstructor
public abstract class BaseDocument {

  @Id private String id;

  @CreatedDate private Instant createdAt;

  @LastModifiedDate private Instant updatedAt;
}
