package io.dsub.discogsdata.batch.aspect.entity;

import io.dsub.discogsdata.batch.aspect.BatchAspect;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class EntityValidationAspect extends BatchAspect {

  @Around("entity() && (setter() || constructor() || wither())")
  public Object replaceBlankStringToNullValue(ProceedingJoinPoint pjp) throws Throwable {
    Object[] normalizedArgs = pjp.getArgs();
    for (int i = 0; i < pjp.getArgs().length; i++) {
      Object arg = normalizedArgs[i];
      if (arg instanceof String) {
        String strArg = ((String) arg).trim();
        if (strArg.isBlank()) {
          normalizedArgs[i] = null;
        }
      }
    }
    return pjp.proceed(normalizedArgs);
  }
}