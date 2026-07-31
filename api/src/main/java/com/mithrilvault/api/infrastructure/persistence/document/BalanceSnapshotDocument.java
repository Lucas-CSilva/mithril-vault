package com.mithrilvault.api.infrastructure.persistence.document;

import java.time.LocalDate;
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
@Document(collection = "balance_snapshots")
public class BalanceSnapshotDocument extends BaseDocument {
  private String ownerId;
  private String accountId;
  private LocalDate asOfDate;
  private Long balance;
  private String throughTransactionId;
}
