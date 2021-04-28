package io.dsub.discogsdata.aspect.controller;

import io.dsub.discogsdata.aspect.RestControllerAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RestControllerLogger extends RestControllerAspect {

    private final Function<Map<String, String[]>, String> stringifyMap = map ->
            map.entrySet().stream()
                    .map(entry -> String.format(
                            "%s >> %s", entry.getKey(), String.join(",", entry.getValue())))
                    .collect(Collectors.joining(", "));

    @Around("restControllers()")
    public Object logRestControllerRequest(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String params = stringifyMap.apply(request.getParameterMap());
        if (!params.isBlank()) {
            params = "[" + params + "]";
        }

        long start = System.currentTimeMillis();

        try {
            return proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());
        } finally {
            long timeTook = System.currentTimeMillis() - start;

            log.debug("Request: {} {}{} < {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    params,
                    request.getRemoteHost() + ":" + request.getRemotePort(),
                    timeTook);
        }
    }
}
