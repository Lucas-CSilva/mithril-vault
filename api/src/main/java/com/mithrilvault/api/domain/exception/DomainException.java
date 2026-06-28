package com.mithrilvault.api.domain.exception;

import com.mithrilvault.api.domain.model.DomainError;
import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {

  private final DomainError error;

  protected DomainException(DomainError error) {
    super(error.message());
    this.error = error;
  }
}
