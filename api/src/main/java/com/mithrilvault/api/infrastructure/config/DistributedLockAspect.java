package com.mithrilvault.api.infrastructure.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect implements EmbeddedValueResolverAware {
  private StringValueResolver resolver;

  private final LockProvider lockProvider;
  private final ExpressionParser parser = new SpelExpressionParser();

  @Override
  public void setEmbeddedValueResolver(@NonNull StringValueResolver resolver) {
    this.resolver = resolver;
  }

  @Around("@annotation(distributedLock)")
  public Object lockMethod(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
      throws Throwable {
    Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

    // Reactive @Scheduled methods are invoked once at startup to obtain a Publisher, then
    // resubscribed on every tick (Spring's ScheduledAnnotationReactiveSupport) — so the lock
    // acquire/release must live inside a deferred chain, not run eagerly around it, or it only
    // ever fires once, at boot, instead of once per actual scheduled run.
    if (Mono.class.isAssignableFrom(returnType)) {
      return Mono.defer(
          () -> (Mono<?>) acquireLockAndProceedReactive(joinPoint, distributedLock, returnType));
    }
    if (Flux.class.isAssignableFrom(returnType)) {
      return Flux.defer(
          () -> (Flux<?>) acquireLockAndProceedReactive(joinPoint, distributedLock, returnType));
    }

    return acquireLockAndProceed(joinPoint, distributedLock, returnType);
  }

  private Object acquireLockAndProceedReactive(
      ProceedingJoinPoint joinPoint, DistributedLock distributedLock, Class<?> returnType) {
    try {
      return acquireLockAndProceed(joinPoint, distributedLock, returnType);
    } catch (Throwable e) {
      return Mono.class.isAssignableFrom(returnType) ? Mono.error(e) : Flux.error(e);
    }
  }

  private Object acquireLockAndProceed(
      ProceedingJoinPoint joinPoint, DistributedLock distributedLock, Class<?> returnType)
      throws Throwable {
    String lockName = resolveLockName(joinPoint, distributedLock);

    Optional<SimpleLock> lock =
        lockProvider.lock(
            new LockConfiguration(
                Instant.now(),
                lockName,
                Duration.parse(
                    Objects.requireNonNull(
                        resolver.resolveStringValue(distributedLock.lockAtMostFor()))),
                Duration.parse(
                    Objects.requireNonNull(
                        resolver.resolveStringValue(distributedLock.lockAtLeastFor())))));

    if (lock.isEmpty()) {
      log.warn("Failed to acquire lock: {} ", lockName);
      return emptyResultFor(returnType);
    }

    log.info("Successfully acquired lock: {} ", lockName);

    return executeLockMethod(joinPoint, lock.get(), lockName);
  }

  private String resolveLockName(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
    try {
      StandardEvaluationContext context = new StandardEvaluationContext();
      String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
      Object[] args = joinPoint.getArgs();

      // Bind method parameters to SpEL context
      IntStream.range(0, parameterNames.length)
          .forEach(i -> context.setVariable(parameterNames[i], args[i]));

      String resolvedLockName =
          Optional.ofNullable(resolver.resolveStringValue(distributedLock.lockName()))
              .orElseThrow();

      return parser.parseExpression(resolvedLockName).getValue(context, String.class);
    } catch (Exception e) {
      log.error("Error resolving lock name: {}", e.getMessage(), e);
      throw new IllegalStateException("Failed to resolve lock name", e);
    }
  }

  private Object executeLockMethod(ProceedingJoinPoint joinPoint, SimpleLock lock, String lockName)
      throws Throwable {
    Object result;
    try {
      result = joinPoint.proceed();
    } catch (Throwable e) {
      logExecutionFailure(lockName, e);
      releaseLock(lock, lockName);
      throw e;
    }

    if (result instanceof Mono<?> mono) {
      return mono.doOnError(e -> logExecutionFailure(lockName, e))
          .doFinally(signal -> releaseLock(lock, lockName));
    }
    if (result instanceof Flux<?> flux) {
      return flux.doOnError(e -> logExecutionFailure(lockName, e))
          .doFinally(signal -> releaseLock(lock, lockName));
    }

    releaseLock(lock, lockName);
    return result;
  }

  private void logExecutionFailure(String lockName, Throwable e) {
    log.error("Execution failed under lock: {} ", lockName, e);
  }

  private void releaseLock(SimpleLock lock, String lockName) {
    try {
      lock.unlock();
      log.info("Successfully released lock: {} ", lockName);
    } catch (Exception e) {
      log.error("Failed to release lock: {} ", lockName, e);
    }
  }

  private Object emptyResultFor(Class<?> returnType) {
    if (Mono.class.isAssignableFrom(returnType)) {
      return Mono.empty();
    }

    if (Flux.class.isAssignableFrom(returnType)) {
      return Flux.empty();
    }

    return null;
  }
}
