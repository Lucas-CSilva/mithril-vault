package com.mithrilvault.api.infrastructure.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
  String lockName();

  String lockAtLeastFor() default "PT3S";

  String lockAtMostFor() default "PT60s";
}
