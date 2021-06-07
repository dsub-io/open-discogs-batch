package io.dsub.discogs.batch.aspect.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.domain.release.ReleaseXML;
import io.dsub.discogs.batch.domain.release.ReleaseXML.CreditedArtist;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

class EntityValidationAspectTest {

  final EntityValidationAspect aspect = new EntityValidationAspect();

  TestEntity testEntity;
  CreditedArtist creditedArtist;
  ReleaseXML releaseXML;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    AspectJProxyFactory testEntityAspectJProxyFactory = new AspectJProxyFactory(new TestEntity());
    testEntityAspectJProxyFactory.addAspect(aspect);

    DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
    AopProxy testEntityAopProxy = proxyFactory.createAopProxy(testEntityAspectJProxyFactory);

    testEntity = (TestEntity) testEntityAopProxy.getProxy();
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
  }

  @Test
  void whenBlankStringPassedToWitherMethod__ThenShouldPassNullValue() {
    // when
    testEntity = testEntity.withName("");

    List<ILoggingEvent> logs =
        logSpy.getEvents().stream()
            .filter(log -> log.getLoggerName().equals(EntityValidationAspect.class.getName()))
            .collect(Collectors.toList());

    // then
    assertThat(testEntity.getName()).isEqualTo(null);
    logs.forEach(
        log ->
            assertThat(log)
                .satisfies(theLog -> assertThat(theLog.getLevel()).isEqualTo(Level.DEBUG))
                .satisfies(
                    theLog ->
                        assertThat(theLog.getFormattedMessage())
                            .isEqualTo("found empty string setter param found. setting to null.")));
  }
}