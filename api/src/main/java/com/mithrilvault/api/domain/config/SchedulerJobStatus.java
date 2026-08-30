package com.mithrilvault.api.domain.config;

import lombok.Getter;

@Getter
public enum SchedulerJobStatus {
  CONFLICT("conflict"),
  GENERATED("generated"),
  FAILED("failed"),
  CORRECTED("corrected"),
  SAVED("saved");

  SchedulerJobStatus(String value) {
    this.value = value;
  }

  private final String value;
}
