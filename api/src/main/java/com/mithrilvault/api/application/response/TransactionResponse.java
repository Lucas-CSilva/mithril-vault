package com.mithrilvault.api.application.response;

import com.mithrilvault.api.domain.model.AccountSummary;
import com.mithrilvault.api.domain.model.CardSummary;
import com.mithrilvault.api.domain.model.InvoiceSummary;
import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.model.TransactionType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record TransactionResponse(
    String id,
    TransactionType type,
    Long amount,
    LocalDate date,
    String description,
    String categoryId,
    PaymentMethod paymentMethod,
    String accountId,
    String invoiceId,
    AccountSummary account,
    CardSummary card,
    InvoiceSummary invoice,
    Set<String> tags,
    String notes,
    Instant createdAt) {}
