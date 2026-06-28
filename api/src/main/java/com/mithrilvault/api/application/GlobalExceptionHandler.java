package com.mithrilvault.api.application;

import com.mithrilvault.api.application.response.ErrorResponse;
import com.mithrilvault.api.domain.exception.ConflictException;
import com.mithrilvault.api.domain.exception.DomainException;
import com.mithrilvault.api.domain.exception.ErrorCode;
import com.mithrilvault.api.domain.exception.NotFoundException;
import com.mithrilvault.api.domain.exception.UnauthorizedException;
import com.mithrilvault.api.domain.model.DomainError;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UnauthorizedException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse handleUnauthorized(UnauthorizedException ex) {
    log.warn("Unauthorized: {}", ex.getMessage());
    return ErrorResponse.of(ex.getError());
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleNotFound(NotFoundException ex) {
    log.warn("Not found: {}", ex.getMessage());
    return ErrorResponse.of(ex.getError());
  }

  @ExceptionHandler(ConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleConflict(ConflictException ex) {
    log.warn("Conflict: {}", ex.getMessage());
    return ErrorResponse.of(ex.getError());
  }

  @ExceptionHandler(DomainException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
  public ErrorResponse handleDomain(DomainException ex) {
    log.warn("Domain exception: {}", ex.getMessage());
    return ErrorResponse.of(ex.getError());
  }

  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
  public ErrorResponse handleValidation(WebExchangeBindException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    List<DomainError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                e ->
                    DomainError.ofField(
                        ErrorCode.VALIDATION_FAILED, e.getDefaultMessage(), e.getField()))
            .toList();
    return ErrorResponse.of(errors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
  public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
    log.warn("Constraint violation: {}", ex.getMessage());
    List<DomainError> errors =
        ex.getConstraintViolations().stream()
            .map(
                v ->
                    DomainError.ofField(
                        ErrorCode.VALIDATION_FAILED,
                        v.getMessage(),
                        v.getPropertyPath().toString()))
            .toList();
    return ErrorResponse.of(errors);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    log.warn("Response status exception: {}", ex.getMessage());
    return ResponseEntity.status(ex.getStatusCode())
        .body(ErrorResponse.of(DomainError.of(ErrorCode.INTERNAL_ERROR, ex.getReason())));
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return ErrorResponse.of(
        DomainError.of(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred"));
  }
}
