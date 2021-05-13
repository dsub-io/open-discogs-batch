package io.dsub.discogsdata.batch.aspect.dto;

import org.aspectj.lang.annotation.Pointcut;

public abstract class AbstractDtoAspect {
    @Pointcut("@target(io.dsub.discogsdata.batch.dto.XmlMapped) && execution(void *..set*(String))")
    public void dtoStringSetterMethods() {}
}
