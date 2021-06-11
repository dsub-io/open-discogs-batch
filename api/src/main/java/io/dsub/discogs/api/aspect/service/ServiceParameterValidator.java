package io.dsub.discogs.api.aspect.service;

import io.dsub.discogs.api.aspect.ServiceAspect;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceParameterValidator extends ServiceAspect {

  @Before("services()")
  public void handleError(JoinPoint joinPoint) throws Throwable {
    System.out.println("SIGNATURE >> " + joinPoint.getSignature());
    System.out.println(
        "ARGUMENTS >> "
            + Arrays.stream(joinPoint.getArgs())
                .map(Object::toString)
                .collect(Collectors.joining(",")));
  }
}
