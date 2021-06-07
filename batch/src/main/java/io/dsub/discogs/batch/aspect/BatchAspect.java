package io.dsub.discogs.batch.aspect;

import io.dsub.discogs.common.aspect.BaseAspect;
import org.aspectj.lang.annotation.Pointcut;

public abstract class BatchAspect extends BaseAspect {

  @Pointcut("target(io.dsub.discogs.batch.query.JpaEntityQueryBuilder)")
  public void jpaEntityQueryBuilder() {
  }
}
