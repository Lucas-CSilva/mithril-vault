package com.mithrilvault.api.application.response;

import com.mithrilvault.api.domain.model.AccountType;
import java.time.Instant;

public record AccountResponse(
    String id,
    String name,
    AccountType type,
    String institution,
    Long initialBalance,
    Long currentBalance,
    String color,
    Boolean isActive,
    Instant createdAt) {}
