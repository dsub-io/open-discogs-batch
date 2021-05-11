package io.dsub.discogsdata.batch.aspect.entity;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Slf4j
@Aspect
public class EntityValidationAspect extends AbstractEntityAspect {

  @Around("entityStringSetterMethods()")
  public Object replaceBlankStringToNullValue(ProceedingJoinPoint pjp) throws Throwable {
    Object[] normalizedArgs = new Object[pjp.getArgs().length];
    for (int i = 0; i < pjp.getArgs().length; i++) {
      Object arg = pjp.getArgs()[i];
      if (arg instanceof String && ((String) arg).isBlank()) {
        log.debug("found empty string setter param found. setting to null.");
        normalizedArgs[i] = null;
        continue;
      }
      normalizedArgs[i] = arg;
    }
    return pjp.proceed(normalizedArgs);
  }
}
