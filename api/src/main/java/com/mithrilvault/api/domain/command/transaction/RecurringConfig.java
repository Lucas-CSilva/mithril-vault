package com.mithrilvault.api.domain.command.transaction;

import com.mithrilvault.api.domain.model.TransactionFrequency;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RecurringConfig(@NotNull TransactionFrequency frequency, LocalDate endDate) {}
