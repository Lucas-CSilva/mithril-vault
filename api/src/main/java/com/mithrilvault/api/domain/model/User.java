package com.mithrilvault.api.domain.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record User(
    String id,
    String email,
    String passwordHash,
    String displayName,
    UserStatus status,
    Instant createdAt,
    Long version) {}
