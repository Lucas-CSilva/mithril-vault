package com.mithrilvault.api.infrastructure.persistence.document;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshTokenDocument extends BaseDocument {

  private String userId;
  private String tokenHash;
  private Instant expiresAt;
  private Instant revokedAt;
  private String replacedBy;
}
