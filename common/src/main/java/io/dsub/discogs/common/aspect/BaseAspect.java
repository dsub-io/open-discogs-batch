package io.dsub.discogs.common.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class BaseAspect {

  ///////////////////////////////////////////////////////////////////////////
  // EXECUTIONS
  ///////////////////////////////////////////////////////////////////////////

  @Pointcut("execution(io.dsub.discogs..*.new(..))")
  public void constructor() {}

  @Pointcut("execution(* io.dsub.discogs..*Test(..))")
  public void testMethod() {}

  @Pointcut("execution(* io.dsub.discogs..*(..)) && !constructor()")
  public void anyMethod() {}

  @Pointcut("execution(* io.dsub.discogs..*(.., java.lang.Class, ..)) && !constructor()")
  public void methodsTakeOneOrMoreClass() {}

  @Pointcut("execution(* io.dsub.discogs..set*(..))")
  public void setter() {}

  @Pointcut("execution(* io.dsub.discogs..get*(..))")
  public void getter() {}

  @Pointcut("execution(* io.dsub.discogs..with*(..))")
  public void wither() {}

  ///////////////////////////////////////////////////////////////////////////
  // ANNOTATIONS
  ///////////////////////////////////////////////////////////////////////////

  @Pointcut("@within(org.springframework.stereotype.Service)")
  public void service() {}

  @Pointcut("@within(javax.persistence.Entity)")
  public void entity() {}

  @Pointcut("@within(org.springframework.stereotype.Repository)")
  public void repository() {}

  @Pointcut("@within(org.springframework.stereotype.Controller)")
  public void controller() {}
}
