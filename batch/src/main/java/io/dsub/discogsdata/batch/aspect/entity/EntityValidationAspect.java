package io.dsub.discogsdata.batch.aspect.entity;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class EntityValidationAspect {
  @Around("@within(javax.persistence.Entity) && execution(* *..with*(String, ..)) || @within(javax.persistence.Entity) && execution(* *..set*(String, ..))")
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
