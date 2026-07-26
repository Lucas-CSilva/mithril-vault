package com.mithrilvault.api.domain.command.transaction;

import com.mithrilvault.api.domain.model.PaymentMethod;
import com.mithrilvault.api.domain.model.TransactionMode;
import com.mithrilvault.api.domain.model.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import lombok.Builder;

@Builder
public record CreateTransactionCommand(
    @NotNull TransactionMode mode,
    @NotNull TransactionType type,
    @NotNull @Positive Long amount,
    @NotNull LocalDate date,
    @NotBlank @Size(max = 200) String description,
    String categoryId,
    PaymentMethod paymentMethod,
    String accountId,
    String cardId,
    Set<@Size(max = 50) String> tags,
    @Size(max = 500) String notes,
    @Valid RecurringConfig recurring,
    @Valid InstallmentConfig installment,
    @Valid TransferConfig transfer) {}
