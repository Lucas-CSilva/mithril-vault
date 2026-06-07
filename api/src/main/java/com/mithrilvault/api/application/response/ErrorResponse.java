package com.mithrilvault.api.application.response;

import com.mithrilvault.api.domain.model.DomainError;
import java.time.Instant;
import java.util.List;

public record ErrorResponse(Instant timestamp, List<DomainError> errors) {

  public static ErrorResponse of(DomainError... errors) {
    return new ErrorResponse(Instant.now(), List.of(errors));
  }

  public static ErrorResponse of(List<DomainError> errors) {
    return new ErrorResponse(Instant.now(), errors);
  }
}
