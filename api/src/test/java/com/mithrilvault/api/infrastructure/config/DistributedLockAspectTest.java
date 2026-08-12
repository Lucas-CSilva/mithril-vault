package com.mithrilvault.api.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DistributedLockAspectTest {

  @Mock private LockProvider lockProvider;
  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private MethodSignature methodSignature;
  @Mock private DistributedLock distributedLock;
  @Mock private SimpleLock simpleLock;

  private DistributedLockAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new DistributedLockAspect(lockProvider);
    aspect.setEmbeddedValueResolver(value -> value);

    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getReturnType()).thenReturn(Mono.class);
    when(methodSignature.getParameterNames()).thenReturn(new String[0]);
    when(joinPoint.getArgs()).thenReturn(new Object[0]);

    when(distributedLock.lockName()).thenReturn("'test-lock'");
    when(distributedLock.lockAtMostFor()).thenReturn("PT30S");
    when(distributedLock.lockAtLeastFor()).thenReturn("PT1S");
  }

  @Test
  void reacquiresLock_onEverySubscription_notJustOnce() throws Throwable {
    // Regression guard: Spring's reactive @Scheduled support invokes the annotated method
    // exactly once (at startup) to obtain a Publisher, then resubscribes that same Publisher on
    // every scheduled tick. If lock acquisition ran eagerly outside the returned Mono, it would
    // fire once at boot and never again — silently disabling the distributed lock for every real
    // scheduled run.
    when(joinPoint.proceed()).thenReturn(Mono.empty());
    when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(simpleLock));

    Object result = aspect.lockMethod(joinPoint, distributedLock);

    StepVerifier.create((Mono<?>) result).verifyComplete();
    StepVerifier.create((Mono<?>) result).verifyComplete();

    verify(lockProvider, times(2)).lock(any(LockConfiguration.class));
    verify(joinPoint, times(2)).proceed();
  }

  @Test
  void emitsEmpty_whenLockCannotBeAcquired() throws Throwable {
    when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.empty());

    Object result = aspect.lockMethod(joinPoint, distributedLock);

    StepVerifier.create((Mono<?>) result).verifyComplete();

    verify(joinPoint, times(0)).proceed();
  }
}
