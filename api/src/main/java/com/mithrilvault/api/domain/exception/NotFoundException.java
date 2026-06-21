package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;

public class NotFoundException extends DomainException {

  public NotFoundException(String message) {
    super(DomainError.of(ErrorCode.RESOURCE_NOT_FOUND, message));
  }
}
