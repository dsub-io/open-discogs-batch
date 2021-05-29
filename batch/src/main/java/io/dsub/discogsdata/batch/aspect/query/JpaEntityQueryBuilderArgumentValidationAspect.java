package io.dsub.discogsdata.batch.aspect.query;

import io.dsub.discogsdata.batch.aspect.BatchAspect;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import io.dsub.discogsdata.common.exception.MissingAnnotationException;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class JpaEntityQueryBuilderArgumentValidationAspect extends BatchAspect {

  public static final String SERIAL_VERSION_UID = "SerialVersionUID";

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

  @Around("jpaEntityQueryBuilder() && methodsTakeOneOrMoreClass()")
  public Object jpaEntityValidationAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    checkNullArg(joinPoint.getArgs());
    for (Object arg : joinPoint.getArgs()) {
      if (arg instanceof Class) {
        Class<?> clazz = (Class<?>) arg;
        checkIfTableAnnotationExists(clazz);
        Arrays.stream(clazz.getDeclaredFields()).forEach(this::checkIfColumnAnnotationExists);
      }
    }

    return joinPoint.proceed(joinPoint.getArgs());
  }

  private void checkIfTableAnnotationExists(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(Table.class)) {
      throw new MissingAnnotationException(clazz, Table.class);
    }
  }

  private void checkNullArg(Object[] args) {
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

  private void checkIfColumnAnnotationExists(Field field) {
    if (field.getName().equals(SERIAL_VERSION_UID)) {
      return;
    }
    if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(JoinColumn.class)) {
      throw new MissingAnnotationException(MISSING_COLUMN_ANNOTATION_MSG + field);
    }
  }
}
