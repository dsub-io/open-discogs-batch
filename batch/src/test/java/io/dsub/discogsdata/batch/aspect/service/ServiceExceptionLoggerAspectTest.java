package io.dsub.discogsdata.batch.aspect.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

class ServiceExceptionLoggerAspectTest {

  final ServiceExceptionLoggerAspect serviceExceptionLoggerAspect =
      new ServiceExceptionLoggerAspect();
  @RegisterExtension public LogSpy logSpy = new LogSpy();
  private TestService testService;

  @BeforeEach
  public void setUp() {
    AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(new TestService());
    aspectJProxyFactory.addAspect(serviceExceptionLoggerAspect);

    DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
    AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);

    testService = (TestService) aopProxy.getProxy();
  }

  @Test
  void shouldPrintWhenExceptionIsThrown() {
    // when
    try {
      testService.throwingMethod();
      fail("should have thrown a runtime exception!");
    } catch (Throwable t) {
      assertThat(t.getMessage()).isEqualTo("exception message");
    }

    // gather the log messages...
    List<ILoggingEvent> loggingEventList =
        logSpy.getEvents().stream()
            .filter(iLoggingEvent -> iLoggingEvent.getLevel().isGreaterOrEqual(Level.ERROR))
            .collect(Collectors.toList());

    // then
    assertThat(loggingEventList.size()).isEqualTo(1);

    ILoggingEvent event = loggingEventList.get(0);
    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
    assertThat(event.getFormattedMessage())
        .contains("RuntimeException thrown from class")
        .contains("TestService")
        .contains("throwingMethod")
        .contains("exception message");
  }
}
