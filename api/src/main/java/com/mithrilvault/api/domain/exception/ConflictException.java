package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

  public ConflictException(String message) {
    super(HttpStatus.CONFLICT, DomainError.of("CONFLICT", message));
  }
}
