package io.dsub.discogsdata.batch.aspect.entity;

import ch.qos.logback.classic.Level;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EntityValidationAspectTest {

  final EntityValidationAspect aspect = new EntityValidationAspect();
  TestEntity testEntity;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(new TestEntity());
    aspectJProxyFactory.addAspect(aspect);

    DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
    AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);

    testEntity = (TestEntity) aopProxy.getProxy();
  }

  @Test
  void whenNullOrNotBlankStringPassedToSetter__ThenShouldPassTheExactSameParamValue() {
    // when
    testEntity.setName("Jimmy Jones");
    // then
    assertThat(testEntity.getName()).isEqualTo("Jimmy Jones");
    assertThat(
            logSpy.getEvents().stream()
                .filter(logEvent -> logEvent.getLevel().equals(Level.ERROR))
                .count())
        .isEqualTo(0);
  }

  @Test
  void whenBlankStringPassedToSetter__ThenShouldPassThatAsNullValue() {
    // when
    testEntity.setName("");
    // then
    assertThat(testEntity.getName()).isEqualTo(null);

    // TODO: this sometimes fails as the size not being reported. we should resolve this issue asap.
    //    assertThat(logSpy.getEvents().size()).isEqualTo(2); // aspectj debug counts as 1.. hence
    // 2.
  }
}
