package com.mithrilvault.api.application.response;

import com.mithrilvault.api.domain.model.DomainError;
import java.util.List;

public record ErrorResponse(List<DomainError> errors) {
  public ErrorResponse(DomainError... errors) {
    this(List.of(errors));
  }
}
