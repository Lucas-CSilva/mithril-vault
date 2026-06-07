package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

  private final HttpStatus status;
  private final DomainError error;

  protected DomainException(HttpStatus status, DomainError error) {
    super(error.message());
    this.status = status;
    this.error = error;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public DomainError getError() {
    return error;
  }
}
