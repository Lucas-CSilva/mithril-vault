package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import org.springframework.http.HttpStatus;

public class BusinessException extends DomainException {

  public BusinessException(ErrorCode code, String message) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, DomainError.of(code, message));
  }
}
