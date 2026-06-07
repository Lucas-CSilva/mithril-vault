package com.mithrilvault.api.application;

import com.mithrilvault.api.application.response.ErrorResponse;
import com.mithrilvault.api.domain.exception.DomainException;
import com.mithrilvault.api.domain.model.DomainError;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
    log.warn("Domain exception: {}", ex.getMessage());
    return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getError()));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    List<DomainError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> DomainError.ofField("VALIDATION_FAILED", e.getDefaultMessage(), e.getField()))
            .toList();
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorResponse.of(errors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    log.warn("Constraint violation: {}", ex.getMessage());
    List<DomainError> errors =
        ex.getConstraintViolations().stream()
            .map(
                v ->
                    DomainError.ofField(
                        "VALIDATION_FAILED", v.getMessage(), v.getPropertyPath().toString()))
            .toList();
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorResponse.of(errors));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    log.warn("Response status exception: {}", ex.getMessage());
    return ResponseEntity.status(ex.getStatusCode())
        .body(ErrorResponse.of(DomainError.of("HTTP_ERROR", ex.getReason())));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(DomainError.of("INTERNAL_ERROR", "An unexpected error occurred")));
  }
}
