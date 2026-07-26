package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;

public class NotImplementedException extends DomainException {

  public NotImplementedException(String message) {
    super(DomainError.of(ErrorCode.NOT_IMPLEMENTED, message));
  }
}
