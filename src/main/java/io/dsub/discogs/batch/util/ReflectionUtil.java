package io.dsub.discogs.batch.util;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionUtil {

  public static void normalizeStringFields(Object target) {
    normalizeStringFields(target, ReflectionUtil::normalizeLegacyString);
  }

  public static void normalizeReleaseStringFields(Object target) {
    normalizeStringFields(target, DiscogsStringNormalizer::normalizeNullable);
  }

  private static void normalizeStringFields(Object target, UnaryOperator<String> normalizer) {
    if (target == null) {
      return;
    }
    List<Field> fields = getDeclaredFields(target);
    fields.forEach(field -> doNormalizeString(field, target, normalizer));
  }

  private static void doNormalizeString(
      Field field, Object target, UnaryOperator<String> normalizer) {
    Object o = getValue(target, field);
    if (o == null) {
      return;
    }
    if (o instanceof String) {
      setFieldValue(target, field, normalizer.apply((String) o));
    } else if (List.class.isAssignableFrom(o.getClass())) {
      List<?> list = (List<?>) o;
      if (list.isEmpty()) {
        return;
      }
      boolean stringList =
          list.stream().filter(Objects::nonNull).allMatch(String.class::isInstance);
      if (stringList) {
        List<String> normalized =
            list.stream()
                .filter(Objects::nonNull)
                .map(String.class::cast)
                .map(normalizer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        setFieldValue(target, field, normalized.isEmpty() ? null : normalized);
      } else {
        list.forEach(value -> normalizeStringFields(value, normalizer));
      }
    } else {
      List<Field> subItemFields = getDeclaredFields(o);
      for (Field subItemField : subItemFields) {
        doNormalizeString(subItemField, o, normalizer);
      }
    }
  }

  private static String normalizeLegacyString(String value) {
    String normalized = value.trim();
    return normalized.isBlank() ? null : normalized;
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
