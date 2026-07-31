package com.mithrilvault.api.infrastructure.persistence.document;

import com.mithrilvault.api.domain.model.*;
import java.time.LocalDate;
import java.util.Set;
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
@Document(collection = "transactions")
public class TransactionDocument extends BaseDocument {
  private String ownerId;
  private TransactionType type;
  private Long amount;
  private LocalDate date;
  private String description;
  private String categoryId;
  private PaymentMethod paymentMethod;
  private String accountId;
  private String invoiceId;
  private AccountSummary account;
  private CardSummary card;
  private InvoiceSummary invoice;
  private Set<String> tags;
  private String notes;
  private Boolean isRecurring;
  private String recurringSeriesId;
  private Installment installment;
  private String transferPairId;
  private String importHash;
  private String fitid;
  private ImportSource importSource;
  private Boolean isReconciliation;
  private Set<String> appliedProjections;
}
