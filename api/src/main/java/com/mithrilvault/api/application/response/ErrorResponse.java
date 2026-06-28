package com.mithrilvault.api.application.response;

import com.mithrilvault.api.domain.model.DomainError;
import java.util.List;

public record ErrorResponse(List<DomainError> errors) {

  public static ErrorResponse of(DomainError... errors) {
    return new ErrorResponse(List.of(errors));
  }

  public static ErrorResponse of(List<DomainError> errors) {
    return new ErrorResponse(errors);
  }
}
