package com.mithrilvault.api.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mithrilvault.api.application.security.CurrentOwnerId;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.test.StepVerifier;

class CurrentOwnerIdArgumentResolverTest {

  private final CurrentOwnerIdArgumentResolver resolver = new CurrentOwnerIdArgumentResolver();

  @Test
  void supportsParameter_whenAnnotatedString_returnsTrue() throws NoSuchMethodException {
    assertThat(resolver.supportsParameter(annotatedOwnerIdParameter())).isTrue();
  }

  @Test
  void supportsParameter_whenPlainString_returnsFalse() throws NoSuchMethodException {
    Method method = TestController.class.getMethod("plain", String.class);
    assertThat(resolver.supportsParameter(new MethodParameter(method, 0))).isFalse();
  }

  @Test
  void resolveArgument_extractsSubjectFromJwtPrincipal() throws NoSuchMethodException {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getSubject()).thenReturn("owner-123");
    var authentication = new TestingAuthenticationToken(jwt, null);
    var context =
        ReactiveSecurityContextHolder.withSecurityContext(
            reactor.core.publisher.Mono.just(new SecurityContextImpl(authentication)));

    resolver
        .resolveArgument(annotatedOwnerIdParameter(), null, null)
        .contextWrite(context)
        .as(StepVerifier::create)
        .expectNext("owner-123")
        .verifyComplete();
  }

  private MethodParameter annotatedOwnerIdParameter() throws NoSuchMethodException {
    Method method = TestController.class.getMethod("annotated", String.class);
    return new MethodParameter(method, 0);
  }

  static class TestController {
    public void annotated(@CurrentOwnerId String ownerId) {}

    public void plain(String ownerId) {}
  }
}
