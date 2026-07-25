package com.mithrilvault.api.infrastructure.config;

import com.mithrilvault.api.application.security.CurrentOwnerId;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CurrentOwnerIdArgumentResolver extends HandlerMethodArgumentResolverSupport {

  public CurrentOwnerIdArgumentResolver() {
    super(ReactiveAdapterRegistry.getSharedInstance());
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentOwnerId.class)
        && String.class.equals(parameter.getParameterType());
  }

  @Override
  public Mono<Object> resolveArgument(
      MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> ((Jwt) context.getAuthentication().getPrincipal()).getSubject());
  }
}
