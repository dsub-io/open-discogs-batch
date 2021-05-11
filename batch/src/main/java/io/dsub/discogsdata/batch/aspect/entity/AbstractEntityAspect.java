package io.dsub.discogsdata.batch.aspect.entity;

import org.aspectj.lang.annotation.Pointcut;

public abstract class AbstractEntityAspect {
  @Pointcut("@target(javax.persistence.Entity) && execution(void *..set*(String))")
  public void entityStringSetterMethods() {}
}
