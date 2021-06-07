package io.dsub.discogs.batch.aspect.query;

import static io.dsub.discogs.batch.aspect.query.JpaEntityQueryBuilderArgumentValidationAspect.CANNOT_ACCEPT_NULL_ARG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.aspect.service.TestService;
import io.dsub.discogs.batch.query.JpaEntityQueryBuilder;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.aspect.app.ApplicationExceptionLoggerAspect;
import io.dsub.discogs.batch.aspect.entity.TestEntity;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.exception.InvalidArgumentException;
import io.dsub.discogs.common.exception.MissingAnnotationException;
import java.util.stream.IntStream;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

class JpaEntityQueryBuilderArgumentValidationAspectTest {

  final JpaEntityQueryBuilderArgumentValidationAspect validator = new JpaEntityQueryBuilderArgumentValidationAspect();
  final ApplicationExceptionLoggerAspect loggerAspect = new ApplicationExceptionLoggerAspect();
  JpaEntityQueryBuilder<BaseEntity> queryBuilder;
  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(
        mock(JpaEntityQueryBuilder.class));
    aspectJProxyFactory.addAspect(loggerAspect);
    aspectJProxyFactory.addAspect(validator);
    DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
    AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);
    this.queryBuilder = (JpaEntityQueryBuilder<BaseEntity>) aopProxy.getProxy();
  }

  @Test
  void whenNullPassed__ShouldNotInvokeTargetMethod() {
    assertThrows(InvalidArgumentException.class,
        () -> queryBuilder.getTableName(null));
    assertThrows(InvalidArgumentException.class,
        () -> queryBuilder.getUniqueConstraintColumns(null));
    verifyNoInteractions(queryBuilder);
  }

  @Test
  void whenJpaEntityFieldValidationAdvice__ShouldCheckNullArg() {
    Throwable t = catchThrowable(() -> queryBuilder.getMappings(null, true));
    assertThat(t).isInstanceOf(InvalidArgumentException.class)
        .hasMessageContaining(CANNOT_ACCEPT_NULL_ARG);
    assertThat(logSpy.countExact(Level.ERROR)).isOne();
  }

  @Test
  void whenJpaEntityValidationAdvice__ShouldCheckNullArg() {
    Throwable t = catchThrowable(
        () -> queryBuilder.getMappingsOutsideUniqueConstraints(null, false));
    assertThat(t).isInstanceOf(InvalidArgumentException.class)
        .hasMessageContaining(CANNOT_ACCEPT_NULL_ARG);
    assertThat(logSpy.countExact(Level.ERROR)).isOne();
  }

  @Test
  void whenAdvicesAcceptJoinPoint__ShouldCallGetArgs() {
    Object[] args = IntStream.of(0, 5)
        .mapToObj(i -> mock(Object.class))
        .toArray();

    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);

    Assertions.assertDoesNotThrow(() -> validator.jpaEntityValidationAdvice(pjp));
    verify(pjp, times(3)).getArgs();
  }

  @Test
  void whenAdvicesAcceptJoinPoint__ShouldThrowIfAnyOfArgumentIsNull() {
    Object[] args = IntStream.of(0, 5)
        .mapToObj(i -> i % 2 > 0 ? mock(Object.class) : null)
        .toArray();

    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);

    Throwable t1 = catchThrowable(() -> validator.jpaEntityValidationAdvice(pjp));

    assertThat(t1).hasMessage(CANNOT_ACCEPT_NULL_ARG);

    verify(pjp, times(1)).getArgs();
  }

  @Test
  void whenAdvicesAcceptJoinPoint__ShouldReturnDelegatedResult() throws Throwable {
    Object[] args = new Object[]{TestEntityTwo.class};
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);
    given(pjp.proceed(args)).willReturn("hi");
    assertThat(validator.jpaEntityValidationAdvice(pjp)).isEqualTo("hi");
    verify(pjp, times(1)).proceed(args);
    verify(pjp, times(3)).getArgs();
  }

  @Test
  void whenArgumentIsClass__ShouldCheckAccordingly() throws Throwable {
    Object[] args = new Object[]{TestEntityTwo.class};
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);
    given(pjp.proceed(args)).willReturn("potato_man");
    assertThat(validator.jpaEntityValidationAdvice(pjp)).isEqualTo("potato_man");
  }

  @Test
  void whenClassHasNoTableAnnotation__ShouldThrow() {
    Object[] args = new Object[]{MalformedClass.class};
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);
    Throwable t = catchThrowable(() -> validator.jpaEntityValidationAdvice(pjp));
    assertThat(t)
        .isInstanceOf(MissingAnnotationException.class)
        .hasMessageContaining(MalformedClass.class.getName())
        .hasMessageContaining("@Table");
  }

  @Test
  void whenAdviceGetsClass__ShouldNotTouchArgument() {
    try {
      Object[] args = new Object[]{TestEntityTwo.class};
      ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
      given(pjp.getArgs()).willReturn(args);
      when(pjp.proceed(args)).thenReturn("hi");
      assertThat(validator.jpaEntityValidationAdvice(pjp)).isEqualTo("hi");
      verify(pjp, times(1)).proceed(args);
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenAdviceGetsField__ShouldNotTouchArgument() {
    try {
      Object[] args = TestEntity.class.getDeclaredFields();
      ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
      given(pjp.getArgs()).willReturn(args);
      when(pjp.proceed(args)).thenReturn("hi");
      assertThat(validator.jpaEntityValidationAdvice(pjp)).isEqualTo("hi");
      verify(pjp, times(1)).proceed(args);
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenJpaEntityQueryBuilderTakesClass__ShouldRecognize() {
    Throwable t = catchThrowable(() -> queryBuilder.getTableName(null));
    assertThat(t)
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessage(CANNOT_ACCEPT_NULL_ARG);
    verifyNoInteractions(queryBuilder);
  }

  @Test
  void whenMethodTakeClassOrFieldInvokedFromOtherPackage__ShouldNotInterfere() {
    TestService testService = new TestService();
    assertDoesNotThrow(() -> testService.methodTakeClass(null));
    assertDoesNotThrow(() -> testService.methodTakeField(null));
  }

  // class to be tested
  protected static class MalformedClass {

    private String invalidField;
    @Column
    private String validField;
  }

  @Table(name = "test_entity_two")
  private static class TestEntityTwo {

    @Id
    @Column
    private Long id;

    @Column(name = "hello")
    private String hello;

    @JoinColumn(name = "where", referencedColumnName = "id")
    private TestEntityTwo otherReference;
  }
}