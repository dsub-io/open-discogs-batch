package io.dsub.discogsdata.batch.aspect.service;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;

@Slf4j
@Aspect
@Component
public class ServiceExceptionLoggerAspect extends AbstractServiceAspect {

  @Around("services()")
  public Object handleError(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      return joinPoint.proceed(joinPoint.getArgs());
    } catch (Throwable e) {
      String msg =
          String.format(
              "%s thrown from class %s on method %s %s! reason: [%s]",
              e.getClass().getSimpleName(),
              joinPoint.getSignature().getDeclaringType().getName(),
              Modifier.toString(joinPoint.getSignature().getModifiers()),
              joinPoint.getSignature().getName(),
              e.getMessage());
      log.error(msg);
      throw e;
    }
  }
}
