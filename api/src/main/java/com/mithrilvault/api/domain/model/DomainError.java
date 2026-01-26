package com.mithrilvault.api.domain.model;

import lombok.Builder;

@Builder
public record DomainError(String message) {}
