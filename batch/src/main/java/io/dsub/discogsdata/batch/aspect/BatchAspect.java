package io.dsub.discogsdata.batch.aspect;

import io.dsub.discogsdata.common.aspect.BaseAspect;
import org.aspectj.lang.annotation.Pointcut;

public abstract class BatchAspect extends BaseAspect {

  @Pointcut("target(io.dsub.discogsdata.batch.query.JpaEntityQueryBuilder)")
  public void jpaEntityQueryBuilder() {
  }
}
