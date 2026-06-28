package com.mithrilvault.api.infrastructure.config;

import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext;
import io.micrometer.context.ContextRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

  @Bean
  CurrentTraceContext.ScopeDecorator mdcScopeDecorator() {
    return MDCScopeDecorator.get();
  }

  @Bean
  ObservationAwareSpanThreadLocalAccessor observationAwareSpanThreadLocalAccessor(Tracer tracer) {
    ObservationAwareSpanThreadLocalAccessor accessor =
        new ObservationAwareSpanThreadLocalAccessor(tracer);
    ContextRegistry.getInstance().registerThreadLocalAccessor(accessor);
    return accessor;
  }
}
