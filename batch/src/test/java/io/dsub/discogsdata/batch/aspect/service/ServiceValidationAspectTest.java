package io.dsub.discogsdata.batch.aspect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

class ServiceValidationAspectTest {

  final ServiceValidationAspect serviceValidationAspect = new ServiceValidationAspect();
  private TestService testService;

  @BeforeEach
  public void setUp() {
    AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(new TestService());
    aspectJProxyFactory.addAspect(serviceValidationAspect);

    DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
    AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);

    testService = (TestService) aopProxy.getProxy();
  }

  @Test
  void throwOnNullArgument() {
    try {
      testService.someMethod(null);
      fail("Should have thrown an exception when null passed as an argument!");
    } catch (InvalidArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("found null argument");
    }
  }

  @Test
  void throwOnBlankStringArgument() {
    try {
      testService.someMethod("");
      fail("Should have thrown an exception when blank string passed as an argument!");
    } catch (InvalidArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("found blank string argument");
    }
  }

  @Test
  void shouldPassIfValidStringArgumentProvided() {
    assertDoesNotThrow(() -> testService.someMethod("hi"));
  }
}
