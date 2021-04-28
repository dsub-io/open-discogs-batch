package io.dsub.discogsdata.aspect;

import org.aspectj.lang.annotation.Pointcut;

public abstract class ServiceAspect {
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void services() {
    }
}