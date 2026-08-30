package com.mithrilvault.api.infrastructure.persistence.document;

import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.model.TransactionFrequency;
import com.mithrilvault.api.domain.model.TransactionType;
import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Document(collection = "recurring_transaction_series")
public class RecurringTransactionSeriesDocument extends BaseDocument {
  private String ownerId;
  private String recurringSeriesId;
  private TransactionFrequency frequency;
  private LocalDate endDate;
  private LocalDate nextOccurrenceDate;
  private TransactionType type;
  private Long amount;
  private String description;
  private String categoryId;
  private PaymentMethod paymentMethod;
  private String accountId;
  private Set<String> tags;
  private String notes;
}
