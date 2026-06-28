package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;

public class ForbiddenException extends DomainException {

  public ForbiddenException(String message) {
    super(DomainError.of(ErrorCode.FORBIDDEN, message));
  }
}
