package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends DomainException {

  public UnauthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, DomainError.of(ErrorCode.UNAUTHORIZED, message));
  }
}
