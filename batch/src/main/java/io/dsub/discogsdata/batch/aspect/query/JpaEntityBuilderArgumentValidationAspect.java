package io.dsub.discogsdata.batch.aspect.query;

import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import io.dsub.discogsdata.common.exception.MissingAnnotationException;
import java.lang.reflect.Field;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class JpaEntityBuilderArgumentValidationAspect {

  public static final String MISSING_COLUMN_ANNOTATION_MSG =
      "expected either @Column or @JoinColumn on field ";

  public static final String BLANK_NAME_MSG_FMT =
      "name value for %s annotation cannot be blank: ";

  public static final String BLANK_COLUMN_NAME_MSG =
      String.format(BLANK_NAME_MSG_FMT, "@Column");

  public static final String BLANK_JOIN_COLUMN_NAME_MSG =
      String.format(BLANK_NAME_MSG_FMT, "@JoinColumn");

  public static final String BLANK_CONSTRAINT_NAME_MSG =
      String.format(BLANK_NAME_MSG_FMT, "@UniqueConstraint");

  public static final String CANNOT_ACCEPT_NULL_ARG =
      "cannot accept null argument: ";

  @Around("execution(* io.dsub.discogsdata.batch.query..*(.., java.lang.reflect.Field, ..))")
  public Object jpaEntityFieldValidationAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    checkNullArg(joinPoint.getArgs());
    for (Object arg : joinPoint.getArgs()) {
      if (arg instanceof Field) {
        Field field = (Field) arg;
        checkIfColumnAnnotationExists(field);
      }
    }
    return joinPoint.proceed(joinPoint.getArgs());
  }

  @Around("execution(* io.dsub.discogsdata.batch.query..*(.., java.lang.Class, ..))")
  public Object jpaEntityValidationAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    checkNullArg(joinPoint.getArgs());
    for (Object arg : joinPoint.getArgs()) {
      if (arg instanceof Class) {
        Class<?> clazz = (Class<?>) arg;
        checkIfTableAnnotationExists(clazz);
      }
    }

    return joinPoint.proceed(joinPoint.getArgs());
  }

  public void checkIfTableAnnotationExists(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(Table.class)) {
      throw new MissingAnnotationException(clazz, Table.class);
    }
  }

  public void checkNullArg(Object[] args) {
    if (args != null) {
      for (Object arg : args) {
        if (arg == null) {
          throw new InvalidArgumentException(CANNOT_ACCEPT_NULL_ARG);
        }
      }
    } else {
      throw new InvalidArgumentException(CANNOT_ACCEPT_NULL_ARG);
    }
  }

  public void checkIfColumnAnnotationExists(Field field) {
    if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(JoinColumn.class)) {
      throw new MissingAnnotationException(MISSING_COLUMN_ANNOTATION_MSG + field);
    }
  }
}
