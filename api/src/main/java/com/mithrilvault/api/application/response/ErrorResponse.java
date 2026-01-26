package com.mithrilvault.api.application.response;

import com.mithrilvault.api.domain.model.DomainError;

public record ErrorResponse(DomainError... errors) {}
