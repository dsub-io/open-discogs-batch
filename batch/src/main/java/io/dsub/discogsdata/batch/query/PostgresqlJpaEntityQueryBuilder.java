package io.dsub.discogsdata.batch.query;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.exception.MissingAnnotationException;
import java.util.stream.Collectors;
import javax.persistence.Column;

public class PostgresqlJpaEntityQueryBuilder extends AbstractJpaEntityQueryBuilder<BaseEntity> {

  public static final String INSERT_QUERY_FORMAT =
      "INSERT INTO %s(%s) VALUES (%s) ";

  public static final String UPSERT_QUERY_FORMAT = INSERT_QUERY_FORMAT +
      "ON CONFLICT (%s) DO UPDATE SET %s WHERE %s";

  @Override
  public String getInsertQuery(Class<? extends BaseEntity> targetClass) {
    String tblName = getTableName(targetClass);
    boolean includeId = true;

    if (isEntityIdNotManaged(targetClass)) {
      if (!hasUniqueConstraintAnnotation(targetClass)) {
        throw new MissingAnnotationException(
            "items without predefined keys must contain unique constraints!");
      }
      includeId = false;
    }
    String fullColumns = String.join(",", getColumns(targetClass, includeId));
    String mappedValues = getValueMappedString(targetClass, includeId);
    return String.format(INSERT_QUERY_FORMAT, tblName, fullColumns, mappedValues);
  }

  @Override
  public String getUpsertQuery(Class<? extends BaseEntity> targetClass, boolean withId) {
    String tblName = getTableName(targetClass);

    boolean includeId = true;
    String fullColumns;
    String mappedValues;
    String unique = String.join(",", getUniqueKeys(targetClass));
    String updateSetColumnsAndValues;
    String targetCriteria;

    // if entity id is generated automatically
    if (isEntityIdNotManaged(targetClass)) {
      if (!hasUniqueConstraintAnnotation(targetClass)) {
        throw new MissingAnnotationException(
            "items without predefined keys must contain unique constraints!");
      }
      updateSetColumnsAndValues = LAST_MODIFIED_COLUMN + EQUALS + COLON + LAST_MODIFIED_FIELD;
      targetCriteria = String.join(SPACE + AND + SPACE,
          getUniqueConstraintsColumnsAndFields(targetClass));
      includeId = false;
    } else { // we know the exact identifier to be inserted.
      updateSetColumnsAndValues = getUpdateSetColumnsAndValues(targetClass);
      targetCriteria = getIdColumnAndFields(targetClass).entrySet().stream()
          .map(entry -> entry.getKey() + EQUALS + COLON + entry.getValue()).collect(
              Collectors.joining(SPACE + AND + SPACE));
    }

    fullColumns = String.join(",", getColumns(targetClass, includeId));
    mappedValues = getValueMappedString(targetClass, includeId);

    return String.format(UPSERT_QUERY_FORMAT, tblName, fullColumns, mappedValues, unique,
        updateSetColumnsAndValues, targetCriteria);
  }

  @Override
  public String getUpdateSetColumnsAndValues(Class<?> targetClass) {
    return getFieldStream(targetClass)
        .filter(field -> !isCreatedAt(field) && !isIdentifier(field))
        .map(field -> {
          if (isLastModifiedAt(field)) {
            return LAST_MODIFIED_COLUMN + EQUALS + COLON + LAST_MODIFIED_FIELD;
          }
          return field.getAnnotation(Column.class).name() + EQUALS + COLON + field.getName();
        })
        .collect(Collectors.joining(" "));
  }
}
