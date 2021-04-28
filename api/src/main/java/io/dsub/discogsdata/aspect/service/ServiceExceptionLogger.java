package io.dsub.discogsdata.aspect.service;

import io.dsub.discogsdata.aspect.ServiceAspect;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class ServiceExceptionLogger extends ServiceAspect {

    @Around("services()")
    public Object handleError(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed(joinPoint.getArgs());
        } catch (Throwable e) {
            Map<String, Object> args = Arrays.stream(joinPoint.getArgs())
                    .collect(Collectors.toMap(
                            o -> o.getClass().getSimpleName(),
                            o -> String.valueOf(o).replaceAll("[\n\t ]", "")));
            log.debug("{} thrown from class {{}} on method {{} {}} with params {}",
                    e.getClass().getSimpleName(),
                    joinPoint.getSignature().getDeclaringType().getName(),
                    Modifier.toString(joinPoint.getSignature().getModifiers()),
                    joinPoint.getSignature().getName(),
                    args);
            throw e;
        }
    }

}

