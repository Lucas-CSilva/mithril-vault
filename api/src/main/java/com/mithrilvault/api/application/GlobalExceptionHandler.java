package com.mithrilvault.api.application;

import com.mithrilvault.api.application.response.ErrorResponse;
import com.mithrilvault.api.domain.model.DomainError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
    String message = ex.getMessage();

    if (message != null && message.contains("Could not resolve placeholder")) {
      String missingVar = extractMissingVariable(message);
      message =
          "Missing required environment variable: "
              + missingVar
              + ". Please configure this variable before starting the application.";
    }

    return new ErrorResponse(DomainError.builder().message(message).build());
  }

  @ExceptionHandler(ResponseStatusException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleResponseStatusException(ResponseStatusException ex) {
    return new ErrorResponse(DomainError.builder().message(ex.getMessage()).build());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleGenericException(Exception ex) {
    return new ErrorResponse(DomainError.builder().message(ex.getMessage()).build());
  }

  private String extractMissingVariable(String message) {
    int start = message.indexOf("'");
    int end = message.lastIndexOf("'");
    if (start != -1 && end != -1 && start < end) {
      return message.substring(start + 1, end);
    }
    return "UNKNOWN_VARIABLE";
  }
}
