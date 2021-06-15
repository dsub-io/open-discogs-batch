package io.dsub.discogs.batch.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.Assert;

public interface JpaEntityHandler {

  default List<Field> getMappedFields(Class<?> clazz) {
    Class<?> ref = clazz;
    List<Field> list = new ArrayList<>();
    while (ref != Object.class) {
      Arrays.stream(ref.getDeclaredFields()).filter(this::isMappedField).forEachOrdered(list::add);
      ref = ref.getSuperclass();
    }
    return list;
  }

  default List<String> getPlainColumns(Class<?> clazz) {
    return getMappedFields(clazz).stream()
        .filter(this::isColumn)
        .map(this::getColumnName)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  default List<String> getJoinColumns(Class<?> clazz) {
    return getMappedFields(clazz).stream()
        .filter(this::isJoinColumn)
        .map(this::getColumnName)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  default List<String> getFieldNames(Class<?> clazz) {
    return getMappedFields(clazz).stream().map(Field::getName).collect(Collectors.toList());
  }

  default List<String> getColumnNames(Class<?> clazz) {
    return getMappedFields(clazz).stream().map(this::getColumnName).collect(Collectors.toList());
  }

  default String getJoinColumnReferencedColumn(Field field) {
    Assert.isTrue(isJoinColumn(field), "field must be join column");
    return field.getAnnotation(JoinColumn.class).referencedColumnName();
  }

  default String getJoinColumnReferencedTable(Field field) {
    Assert.isTrue(isJoinColumn(field), "field must be join column");
    return field.getType().getAnnotation(Table.class).name();
  }

  default String getColumnName(Field field) {
    if (!isMappedField(field)) {
      return null;
    }
    String name;
    if (isColumn(field)) {
      name = field.getAnnotation(Column.class).name();
    } else {
      name = field.getAnnotation(JoinColumn.class).name();
    };
    if (name.isBlank()) {
      name = field.getName();
    }
    return name.replaceAll("([A-Z])", "_$1").toLowerCase();
  }

  default boolean isMappedField(Field field) {
    if (field.isAnnotationPresent(Transient.class)) {
      return false;
    }
    return isColumn(field) || isJoinColumn(field);
  }

  default boolean isColumn(Field field) {
    return field.isAnnotationPresent(Column.class);
  }

  default boolean isJoinColumn(Field field) {
    return field.isAnnotationPresent(JoinColumn.class);
  }

  default boolean hasKnownId(Class<?> clazz) {
    return getMappedFields(clazz).stream()
        .filter(this::isIdField)
        .anyMatch(field -> !field.isAnnotationPresent(GeneratedValue.class));
  }

  default boolean isIdField(Field field) {
    return field.isAnnotationPresent(Id.class);
  }

  default String getTableName(Class<?> clazz) {
    Assert.isTrue(clazz.isAnnotationPresent(Table.class), "class must has table annotation.");
    return clazz.getAnnotation(Table.class).name();
  }

  default Field getLastModifiedField(Class<?> clazz) {
    return getFieldByAnnotation(clazz, LastModifiedDate.class);
  }

  default Field getCreatedAtField(Class<?> clazz) {
    return getFieldByAnnotation(clazz, CreatedDate.class);
  }

  default boolean hasUniqueConstraints(Class<?> clazz) {
    Assert.isTrue(clazz.isAnnotationPresent(Table.class), "class must has table annotation.");
    return clazz.getAnnotation(Table.class).uniqueConstraints().length > 0;
  }

  private Field getFieldByAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
    return getMappedFields(clazz).stream()
        .filter(field -> field.isAnnotationPresent(annotation))
        .findFirst()
        .orElse(null);
  }

  default List<String> getUniqueConstraintColumns(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(Table.class)
        || clazz.getAnnotation(Table.class).uniqueConstraints().length == 0) {
      return Collections.emptyList();
    }
    return Arrays.stream(clazz.getAnnotation(Table.class).uniqueConstraints())
        .map(UniqueConstraint::columnNames)
        .flatMap(Arrays::stream)
        .distinct()
        .collect(Collectors.toList());
  }

  default List<String> getColumnsWithoutConstraints(Class<?> clazz) {
    List<String> uqList = getUniqueConstraintColumns(clazz);
    return getMappedFields(clazz).stream()
        .filter(this::isMappedField)
        .map(this::getColumnName)
        .filter(name -> !uqList.contains(name))
        .collect(Collectors.toList());
  }
}
