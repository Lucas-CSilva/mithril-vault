package com.mithrilvault.api.infrastructure.scheduler;

import com.mithrilvault.api.domain.config.SchedulerJobStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerJobMetrics {

  private final MeterRegistry meterRegistry;

  public void recordOutcome(String jobName, SchedulerJobStatus outcome) {
    Counter.builder("scheduler.job.outcome.total")
        .tag("job", jobName)
        .tag("outcome", outcome.getValue())
        .description(
            "Outcomes recorded by scheduled background jobs, one increment per item processed")
        .register(meterRegistry)
        .increment();
  }
}
