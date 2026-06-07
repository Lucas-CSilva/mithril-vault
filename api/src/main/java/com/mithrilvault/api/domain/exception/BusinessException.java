package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import org.springframework.http.HttpStatus;

public class BusinessException extends DomainException {

  public BusinessException(String code, String message) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, DomainError.of(code, message));
  }
}
