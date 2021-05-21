package io.dsub.discogsdata.batch.aspect.query;

import static io.dsub.discogsdata.batch.aspect.query.JpaEntityBuilderArgumentValidationAspect.CANNOT_ACCEPT_NULL_ARG;
import static io.dsub.discogsdata.batch.aspect.query.JpaEntityBuilderArgumentValidationAspect.MISSING_COLUMN_ANNOTATION_MSG;
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
import io.dsub.discogsdata.batch.aspect.app.ApplicationExceptionLoggerAspect;
import io.dsub.discogsdata.batch.aspect.entity.TestEntity;
import io.dsub.discogsdata.batch.aspect.service.TestService;
import io.dsub.discogsdata.batch.query.JpaEntityQueryBuilder;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import io.dsub.discogsdata.common.exception.MissingAnnotationException;
import java.lang.reflect.Field;
import java.util.stream.IntStream;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

class JpaEntityBuilderArgumentValidationAspectTest {

  // class to be tested
  protected static class MalformedClass {
    private String invalidField;
    @Column
    private String validField;
  }

  private static class TestEntityFieldClass {
    @Id
    @Column
    private Long id;

    @Column(name = "hello")
    private String hello;

    @JoinColumn(name = "where", referencedColumnName = "id")
    private TestEntityFieldClass otherReference;
  }

  final JpaEntityBuilderArgumentValidationAspect validator = new JpaEntityBuilderArgumentValidationAspect();
  final ApplicationExceptionLoggerAspect loggerAspect = new ApplicationExceptionLoggerAspect();
  JpaEntityQueryBuilder<BaseEntity> queryBuilder;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @Captor
  ArgumentCaptor<Object> objArrArgCaptor;

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
    assertThrows(InvalidArgumentException.class, () -> queryBuilder.isIdentifier(null));
    assertThrows(InvalidArgumentException.class, () -> queryBuilder.getFieldStream(null));
    verifyNoInteractions(queryBuilder);
  }

  @Test
  void whenJpaEntityFieldValidationAdvice__ShouldCheckNullArg() {
    Throwable t = catchThrowable(() -> queryBuilder.isIdentifier(null));
    assertThat(t).isInstanceOf(InvalidArgumentException.class)
        .hasMessageContaining(CANNOT_ACCEPT_NULL_ARG);
    assertThat(logSpy.countExact(Level.ERROR)).isOne();
  }

  @Test
  void whenJpaEntityValidationAdvice__ShouldCheckNullArg() {
    Throwable t = catchThrowable(() -> queryBuilder.getUniqueConstraintsColumns(null));
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

    assertDoesNotThrow(() -> validator.jpaEntityValidationAdvice(pjp));
    verify(pjp, times(3)).getArgs();
    assertDoesNotThrow(() -> validator.jpaEntityFieldValidationAdvice(pjp));
    verify(pjp, times(6)).getArgs();
  }

  @Test
  void whenAdvicesAcceptJoinPoint__ShouldThrowIfAnyOfArgumentIsNull() {
    Object[] args = IntStream.of(0, 5)
        .mapToObj(i -> i % 2 > 0 ? mock(Object.class) : null)
        .toArray();

    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);

    Throwable t1 = catchThrowable(() -> validator.jpaEntityValidationAdvice(pjp));
    Throwable t2 = catchThrowable(() -> validator.jpaEntityFieldValidationAdvice(pjp));

    assertThat(t1).hasMessage(CANNOT_ACCEPT_NULL_ARG);
    assertThat(t2).hasMessage(CANNOT_ACCEPT_NULL_ARG);

    verify(pjp, times(2)).getArgs();
  }

  @Test
  void whenAdvicesAcceptJoinPoint__ShouldReturnDelegatedResult() throws Throwable {
    Object[] args = new Object[]{TestEntity.class};
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);
    given(pjp.proceed(args)).willReturn("hi");
    assertThat(validator.jpaEntityFieldValidationAdvice(pjp)).isEqualTo("hi");
    verify(pjp, times(1)).proceed(args);
    verify(pjp, times(3)).getArgs();
    assertThat(validator.jpaEntityValidationAdvice(pjp)).isEqualTo("hi");
    verify(pjp, times(2)).proceed(args);
    verify(pjp, times(6)).getArgs();
  }

  @Test
  void whenArgumentIsField__ShouldCheckAccordingly() throws Throwable {
    Object[] args = TestEntityFieldClass.class.getDeclaredFields();
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);
    given(pjp.proceed(args)).willReturn("potato_man");
    assertThat(validator.jpaEntityFieldValidationAdvice(pjp)).isEqualTo("potato_man");
  }

  @Test
  void whenArgumentIsClass__ShouldCheckAccordingly() throws Throwable {
    Object[] args = new Object[]{TestEntity.class};
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    given(pjp.getArgs()).willReturn(args);
    given(pjp.proceed(args)).willReturn("potato_man");
    assertThat(validator.jpaEntityFieldValidationAdvice(pjp)).isEqualTo("potato_man");
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
  void whenValidClassGetsValidated__ShouldNotThrow() {
    assertDoesNotThrow(() -> validator.checkIfTableAnnotationExists(TestEntity.class));
  }


  @Test
  void whenAdviceGetsClass__ShouldNotTouchArgument() {
    try {
      Object[] args = new Object[]{TestEntity.class};
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
  void whenNullFieldArg__ShouldCheckNullArguments() {
    try {
      Object[] args = IntStream.of(0, 5)
          .mapToObj(i -> i % 2 == 0 ? mock(Object.class) : null)
          .toArray();
      ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
      when(pjp.getArgs()).thenReturn(args);

      Throwable t = catchThrowable(() -> validator.jpaEntityFieldValidationAdvice(pjp));
      assertThat(t)
          .isInstanceOf(InvalidArgumentException.class)
          .hasMessage(CANNOT_ACCEPT_NULL_ARG);
      verify(pjp, times(1)).getArgs();
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenArgIsInvalidField__ShouldThrow() {
    try {
      Object[] args = MalformedClass.class.getDeclaredFields();
      ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
      when(pjp.getArgs()).thenReturn(args);
      Throwable t = catchThrowable(() -> validator.jpaEntityFieldValidationAdvice(pjp));
      assertThat(t)
          .isInstanceOf(MissingAnnotationException.class)
          .hasMessageContaining("@Column", "@JoinColumn");
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenValidField__ShouldPassSameArg() {
    try {
      Object[] args = TestEntityFieldClass.class.getDeclaredFields();
      ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
      when(pjp.getArgs()).thenReturn(args);
      when(pjp.proceed(args)).thenReturn("red_evil");
      Object result = validator.jpaEntityFieldValidationAdvice(pjp);
      assertThat(result).isEqualTo("red_evil");
      verify(pjp, times(1)).proceed(args);
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenCheckNullArg__ShouldThrowIfNullPassed() {
    Throwable t1 = catchThrowable(() -> validator.checkNullArg(null));
    assertThat(t1)
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessage(CANNOT_ACCEPT_NULL_ARG);
    Throwable t2 = catchThrowable(() -> validator.checkNullArg(new Object[]{null}));
    assertThat(t2)
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessage(CANNOT_ACCEPT_NULL_ARG);
  }

  @Test
  void whenCheckNullArg__ShouldNotThrowIfNotNullPassed() {
    Object[] args = new Object[]{new Object()};
    assertDoesNotThrow(() -> validator.checkNullArg(args));
  }

  @Test
  void whenCheckFieldWithoutColumn__ShouldThrow() {
    try {
      Field field = MalformedClass.class.getDeclaredField("invalidField");
      Throwable t = catchThrowable(() -> validator.checkIfColumnAnnotationExists(field));
      assertThat(t)
          .isInstanceOf(MissingAnnotationException.class)
          .hasMessage(MISSING_COLUMN_ANNOTATION_MSG + field);
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenCheckFieldWithColumn__ShouldNotThrow() {
    try {
      Field joinColumnField = TestEntityFieldClass.class.getDeclaredField("otherReference");
      Field columnField = TestEntityFieldClass.class.getDeclaredField("hello");
      assertDoesNotThrow(() -> validator.checkIfColumnAnnotationExists(joinColumnField));
      assertDoesNotThrow(() -> validator.checkIfColumnAnnotationExists(columnField));
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenJpaEntityQueryBuilderTakesField__ShouldRecognize() {
    try {
      Field field = MalformedClass.class.getDeclaredField("invalidField");
      Throwable t = catchThrowable(() -> queryBuilder.isIdentifier(field));
      assertThat(t)
          .isInstanceOf(MissingAnnotationException.class)
          .hasMessage(MISSING_COLUMN_ANNOTATION_MSG + field);
      verifyNoInteractions(queryBuilder);
    } catch (Throwable t) {
      fail(t);
    }
  }

  @Test
  void whenJpaEntityQueryBuilderTakesClass__ShouldRecognize() {
    Throwable t = catchThrowable(() -> queryBuilder.getTableName(MalformedClass.class));
    assertThat(t)
        .isInstanceOf(MissingAnnotationException.class)
        .hasMessage(new MissingAnnotationException(MalformedClass.class, Table.class).getMessage());
    verifyNoInteractions(queryBuilder);
  }

  @Test
  void whenMethodTakeClassOrFieldInvokedFromOtherPackage__ShouldNotInterfere() {
    TestService testService = new TestService();
    assertDoesNotThrow(() -> testService.methodTakeClass(null));
    assertDoesNotThrow(() -> testService.methodTakeField(null));
  }
}