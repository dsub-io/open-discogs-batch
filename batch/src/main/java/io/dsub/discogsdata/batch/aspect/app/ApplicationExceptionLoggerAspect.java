package io.dsub.discogsdata.batch.aspect.app;

import io.dsub.discogsdata.batch.aspect.BatchAspect;
import java.lang.reflect.Modifier;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ApplicationExceptionLoggerAspect extends BatchAspect {

  @Around("anyMethod()")
  public Object handleError(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      return joinPoint.proceed(joinPoint.getArgs());
    } catch (Throwable e) {
      String msg =
          String.format(
              "%s thrown from class %s on method [%s %s]. reason: [%s]",
              e.getClass().getName(),
              joinPoint.getSignature().getDeclaringType().getName(),
              Modifier.toString(joinPoint.getSignature().getModifiers()),
              joinPoint.getSignature().getName(),
              e.getMessage());
      log.error(msg);
      throw e;
    }
  }
}