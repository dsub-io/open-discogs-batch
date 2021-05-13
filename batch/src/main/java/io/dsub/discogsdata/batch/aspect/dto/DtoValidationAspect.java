package io.dsub.discogsdata.batch.aspect.dto;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class DtoValidationAspect extends AbstractDtoAspect {
    @Around("dtoStringSetterMethods()")
    public Object replaceBlankStringToNullValue(ProceedingJoinPoint pjp) throws Throwable {
        Object[] normalizedArgs = new Object[pjp.getArgs().length];
        for (int i = 0; i < pjp.getArgs().length; i++) {
            Object arg = pjp.getArgs()[i];
            if (arg instanceof String && ((String) arg).isBlank()) {
                normalizedArgs[i] = null;
                continue;
            }
            normalizedArgs[i] = arg;
        }
        return pjp.proceed(normalizedArgs);
    }
}
