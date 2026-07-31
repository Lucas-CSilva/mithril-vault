package com.mithrilvault.api.infrastructure.persistence.document;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.bson.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@org.springframework.data.mongodb.core.mapping.Document(collection = "projection_checkpoints")
public class ProjectionCheckpointDocument {

  @Id private String projectionName;

  // MongoDB change-stream resume tokens are always shaped like {"_data": "..."}. Stored as
  // org.bson.Document (not the abstract org.bson.BsonValue) because Spring Data's
  // MappingMongoConverter has no registered converter for BsonValue and would fail to read it
  // back.
  private Document resumeToken;

  private String lastProcessedTransactionId;
  @LastModifiedDate private Instant updatedAt;

  public void update(Document resumeToken, String lastProcessedTransactionId) {
    this.resumeToken = resumeToken;
    this.lastProcessedTransactionId = lastProcessedTransactionId;
  }
}
