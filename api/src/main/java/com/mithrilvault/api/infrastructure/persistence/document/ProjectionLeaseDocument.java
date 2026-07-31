package com.mithrilvault.api.infrastructure.persistence.document;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Document(collection = "projection_leases")
public class ProjectionLeaseDocument {
  @Id private String projectionName;
  private String instanceId;
  private Instant leaseExpiresAt;
}
