package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {

  public NotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, DomainError.of("RESOURCE_NOT_FOUND", message));
  }
}
