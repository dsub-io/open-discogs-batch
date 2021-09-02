package io.dsub.discogs.batch.util;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionUtil {

  public static void normalizeStringFields(Object target) {
    if (target == null) {
      return;
    }
    List<Field> fields = getDeclaredFields(target);
    fields.forEach(field -> doNormalizeString(field, target));
  }

  private static void doNormalizeString(Field field, Object target) {
    Object o = getValue(target, field);
    if (o == null) {
      return;
    }
    if (o instanceof String) {
      String val = ((String) o).trim();
      if (val.isBlank()) {
        setFieldValue(target, field, null);
      }
    } else if (List.class.isAssignableFrom(o.getClass())) {
      List<?> list = (List<?>) o;
      if (list.isEmpty()) {
        return;
      }
      Object first = list.get(0);
      if (first instanceof String) {
        List<?> normalized =
            list.stream()
                .map(obj -> obj != null && obj.toString().isBlank() ? null : obj)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::trim)
                .collect(Collectors.toList());
        setFieldValue(target, field, normalized.size() == 0 ? null : normalized);
      } else {
        list.forEach(ReflectionUtil::normalizeStringFields);
      }
    } else {
      List<Field> subItemFields = getDeclaredFields(o);
      for (Field subItemField : subItemFields) {
        doNormalizeString(subItemField, o);
      }
    }
  }

  public static List<Field> getDeclaredFields(Object target) {
    return getDeclaredFields(target.getClass());
  }

  public static List<Field> getDeclaredFields(Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
        .filter(field -> !Modifier.isStatic(field.getModifiers()))
        .filter(field -> !Modifier.isFinal(field.getModifiers()))
        .peek(field -> field.setAccessible(true))
        .collect(Collectors.toList());
  }

  public static void setFieldValue(Object target, Field field, Object value) {
    if (target == null) {
      throw new InvalidArgumentException("target object cannot be null");
    }
    if (field == null) {
      throw new InvalidArgumentException("field cannot be null");
    }

    if (value != null) {
      Class<?> fieldType = field.getType();
      Class<?> valueType = value.getClass();

      if (!fieldType.isAssignableFrom(valueType)) {
        throw new InvalidArgumentException(
            "fieldType "
                + fieldType.getSimpleName()
                + " does not match "
                + valueType.getSimpleName());
      }
    }

    if (field.trySetAccessible()) {
      try {
        field.set(target, value);
      } catch (Exception ignored) {
      }
    }
  }

  public static Object getValue(Object target, Field field) {
    if (target == null) {
      throw new InvalidArgumentException("target object cannot be null");
    }
    if (field == null) {
      throw new InvalidArgumentException("field cannot be null");
    }
    try {
      return field.get(target);
    } catch (Exception ignored) {
    }
    return null;
  }

  public static List<Field> getDeclaredFields(Object target, Predicate<Field> condition) {
    return getDeclaredFields(target.getClass(), condition);
  }

  public static List<Field> getDeclaredFields(Class<?> target, Predicate<Field> condition) {
    return getDeclaredFields(target).stream().filter(condition).collect(Collectors.toList());
  }

  public static <T> T invokeNoArgConstructor(Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (NoSuchMethodException e) {
      throw new InvalidArgumentException(
          clazz.getSimpleName() + " does not have no-arg constructor");
    } catch (Throwable e) {
      log.warn("failed to instantiate {}", clazz.getSimpleName());
    }
    return null;
  }
}