package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;

public class ConflictException extends DomainException {

  public ConflictException(String message) {
    super(DomainError.of(ErrorCode.CONFLICT, message));
  }
}
