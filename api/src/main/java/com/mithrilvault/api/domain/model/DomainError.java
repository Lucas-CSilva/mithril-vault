package com.mithrilvault.api.domain.model;

import com.mithrilvault.api.domain.exception.ErrorCode;

public record DomainError(ErrorCode code, String message, String field) {

  public static DomainError of(ErrorCode code, String message) {
    return new DomainError(code, message, null);
  }

  public static DomainError ofField(ErrorCode code, String message, String field) {
    return new DomainError(code, message, field);
  }
}
