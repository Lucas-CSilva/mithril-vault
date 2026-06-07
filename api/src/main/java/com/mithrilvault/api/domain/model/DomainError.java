package com.mithrilvault.api.domain.model;

public record DomainError(String code, String message, String field) {

  public static DomainError of(String code, String message) {
    return new DomainError(code, message, null);
  }

  public static DomainError ofField(String code, String message, String field) {
    return new DomainError(code, message, field);
  }
}
