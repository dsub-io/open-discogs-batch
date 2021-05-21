package io.dsub.discogsdata.batch.aspect.query;

import io.dsub.discogsdata.batch.aspect.app.ApplicationExceptionLoggerAspect;
import io.dsub.discogsdata.batch.query.JpaEntityQueryBuilder;
import io.dsub.discogsdata.batch.query.PostgresqlJpaEntityQueryBuilder;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

class QueryBuilderArgumentValidationAspectTest {

  final QueryBuilderArgumentValidationAspect validator =
      new QueryBuilderArgumentValidationAspect();

  final ApplicationExceptionLoggerAspect loggerAspect =
      new ApplicationExceptionLoggerAspect();

  JpaEntityQueryBuilder<BaseEntity> builder = getProxiedBuilder();

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @SuppressWarnings("unchecked")
  JpaEntityQueryBuilder<BaseEntity> getProxiedBuilder() {
    AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(
        new PostgresqlJpaEntityQueryBuilder());

    aspectJProxyFactory.addAspect(loggerAspect);
    aspectJProxyFactory.addAspect(validator);

    DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();

    AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);

    return (JpaEntityQueryBuilder<BaseEntity>) aopProxy.getProxy();
  }
}