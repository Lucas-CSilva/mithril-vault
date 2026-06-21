package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;

public class UnauthorizedException extends DomainException {

  public UnauthorizedException(String message) {
    super(DomainError.of(ErrorCode.UNAUTHORIZED, message));
  }
}
