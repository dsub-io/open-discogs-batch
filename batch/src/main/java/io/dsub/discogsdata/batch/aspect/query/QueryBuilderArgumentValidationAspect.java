package io.dsub.discogsdata.batch.aspect.query;

import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import io.dsub.discogsdata.common.exception.MissingAnnotationException;
import io.dsub.discogsdata.common.exception.UnknownColumnException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

// TODO: TEST
@Slf4j
@Aspect
//@Component
public class QueryBuilderArgumentValidationAspect {

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

  @Pointcut("execution(* *..*(.., java.lang.reflect.Field, ..))")
  public void methodsTakeField() {
  }

  @Pointcut("execution(* *..*(.., java.lang.Class, ..))")
  public void methodsTakeClass() {
  }

  @Pointcut("target(io.dsub.discogsdata.batch.query.JpaEntityQueryBuilder)")
  public void jpaEntityQueryBuilder() {
  }

  @Around("methodsTakeField() && jpaEntityQueryBuilder()")
  public Object jpaEntityColumnValidationAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    checkNullArg(joinPoint.getArgs());
    for (Object arg : joinPoint.getArgs()) {
      if (arg instanceof Field) {
        Field field = (Field) arg;
        checkIfColumnAnnotationExists(field);
      }
    }
    return joinPoint.proceed(joinPoint.getArgs());
  }

  @Around("methodsTakeClass() && jpaEntityQueryBuilder()")
  public Object jpaEntityValidationAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    checkNullArg(joinPoint.getArgs());
    for (Object arg : joinPoint.getArgs()) {
      if (arg instanceof Class) {
        Class<?> clazz = (Class<?>) arg;
        validateEntity(clazz);
      }
    }

    return joinPoint.proceed(joinPoint.getArgs());
  }

  public void validateEntity(Class<?> clazz) {
    if (clazz == null) {
      throw new InvalidArgumentException(CANNOT_ACCEPT_NULL_ARG);
    }

    if (!clazz.isAnnotationPresent(Table.class)) {
      throw new MissingAnnotationException(clazz, Table.class);
    }

    Table table = clazz.getAnnotation(Table.class);

    if (table.uniqueConstraints().length > 0) {
      List<String> fieldNames = getFieldColumnNames(clazz);
      for (UniqueConstraint constraint : table.uniqueConstraints()) {
        checkUniqueConstraint(constraint, fieldNames, clazz);
      }
    }
  }

  public List<String> getFieldColumnNames(Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
        .map(field -> {
          if (field.isAnnotationPresent(Column.class)) {
            return field.getAnnotation(Column.class).name();
          } else if (field.isAnnotationPresent(JoinColumn.class)) {
            return field.getAnnotation(JoinColumn.class).name();
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public void checkNullArg(Object[] args) {
    if (args == null) {
      throw new InvalidArgumentException(CANNOT_ACCEPT_NULL_ARG);
    }
    for (Object arg : args) {
      if (arg == null) {
        throw new InvalidArgumentException(CANNOT_ACCEPT_NULL_ARG);
      }
    }
  }

  public void checkUniqueConstraint(UniqueConstraint constraint,
      List<String> fieldNames,
      Class<?> targetClass) {

    List<String> unknownColumns = Arrays.stream(constraint.columnNames())
        .filter(columnName -> !fieldNames.contains(columnName))
        .collect(Collectors.toList());
    if (unknownColumns.size() > 0) {
      throw new UnknownColumnException(
          "unknown columns found from @UniqueConstraint " + constraint.name() + " in " +
              targetClass.getSimpleName() + ". check following column entries: (" +
              String.join(", ", unknownColumns) + ")");
    }
  }

  public void checkIfColumnAnnotationExists(Field field) {
    if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(JoinColumn.class)) {
      throw new MissingAnnotationException(MISSING_COLUMN_ANNOTATION_MSG + field);
    }
  }
}
