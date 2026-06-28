package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;

public class BusinessException extends DomainException {

  public BusinessException(ErrorCode code, String message) {
    super(DomainError.of(code, message));
  }
}
