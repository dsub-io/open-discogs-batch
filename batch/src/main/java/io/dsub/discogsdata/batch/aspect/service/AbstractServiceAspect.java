package io.dsub.discogsdata.batch.aspect.service;

import org.aspectj.lang.annotation.Pointcut;

public abstract class AbstractServiceAspect {
  @Pointcut("within(@org.springframework.stereotype.Service *)")
  public void services() {}
}
