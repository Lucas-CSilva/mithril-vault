package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class DomainException extends RuntimeException {

  private final HttpStatus status;
  private final DomainError error;

  protected DomainException(HttpStatus status, DomainError error) {
    super(error.message());
    this.status = status;
    this.error = error;
  }
}
