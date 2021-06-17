package io.dsub.discogs.batch.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.JoinColumn;

public abstract class SqlJpaEntityQueryBuilder<T> implements JpaEntityQueryBuilder<T> {

  public static final String DEFAULT_SQL_INSERT_QUERY_FORMAT = "INSERT INTO %s(%s) SELECT %s";

  public static final String WHERE_CLAUSE_QUERY_FORMAT = "WHERE %s < %d";

  public static final String JOIN_INNER_SEL_FMT = "(SELECT 1 FROM %s WHERE %s = %s)";

  public static final String PRUNE_QUERY_INIT_COND_FMT = "WHERE NOT EXISTS %s";

  public static final String PRUNE_QUERY_ADDITION = "OR NOT EXISTS %s";

  public static final String DEFAULT_SQL_PRUNE_QUERY_FORMAT = "DELETE FROM %s %s";

  @Override
  public String getTemporaryInsertQuery(Class<? extends T> targetClass) {
    List<Field> fields = getMappedFields(targetClass);

    if (!hasKnownId(targetClass)) {
      List<String> idColumns = getIdColumns(targetClass);
      fields =
          fields.stream()
              .filter(field -> !idColumns.contains(getColumnName(field)))
              .collect(Collectors.toList());
    }

    String tblName = getTableName(targetClass) + TMP;
    Field createdAt = getCreatedAtField(targetClass);
    Field lastModified = getLastModifiedAtField(targetClass);
    List<String> columns = fields.stream().map(this::getColumnName).collect(Collectors.toList());
    List<String> values = fields.stream().map(Field::getName).collect(Collectors.toList());
    String mappedValues = getFormattedValueFields(createdAt, lastModified, values);
    return String.format(
        DEFAULT_SQL_INSERT_QUERY_FORMAT, tblName, String.join(",", columns), mappedValues);
  }

  @Override
  public String getPruneQuery(Class<? extends T> targetClass) {
    String whereClause = getRelationExistCountingWhereClause(targetClass);
    String tmpTblName = getTableName(targetClass) + TMP;
    return String.format(DEFAULT_SQL_PRUNE_QUERY_FORMAT, tmpTblName, whereClause);
  }

  protected String getFormattedValueFields(
      Field createdAt, Field lastModifiedAt, List<String> values) {
    String[] parts = new String[values.size()];
    for (int i = 0; i < values.size(); i++) {
      String origin = values.get(i);
      if (createdAt != null && createdAt.getName().equals(origin)) {
        parts[i] = "NOW()";
      } else if (lastModifiedAt != null && lastModifiedAt.getName().equals(origin)) {
        parts[i] = "NOW()";
      } else {
        parts[i] = COLON + origin;
      }
    }
    return String.join(",", parts);
  }

  protected String getRelationExistCountingWhereClause(Class<? extends T> targetClass) {
    List<Field> joinColumnFields = getNotNullableJoinColumnFields(targetClass);

    if (joinColumnFields.size() == 0) {
      return null;
    }

    List<String> innerSelects = new ArrayList<>();
    String srcTblName = getTableName(targetClass) + TMP;

    for (Field field : joinColumnFields) {
      innerSelects.add(getJoinColumnFieldInnerSelect(field, srcTblName));
    }

    String initial = String.format(PRUNE_QUERY_INIT_COND_FMT, innerSelects.get(0)) + SPACE;
    String additions =
        innerSelects.stream()
            .skip(1)
            .map(sel -> String.format(PRUNE_QUERY_ADDITION, sel))
            .collect(Collectors.joining(SPACE));

    return initial + additions;
  }

  protected List<Field> getNotNullableJoinColumnFields(Class<? extends T> targetClass) {
    return getMappedFields(targetClass).stream()
        .filter(field -> field.isAnnotationPresent(JoinColumn.class))
        .filter(field -> !field.getAnnotation(JoinColumn.class).nullable())
        .collect(Collectors.toList());
  }

  private String getJoinColumnFieldInnerSelect(Field joinColumnField, String srcTblName) {
    String refCol = getJoinColumnReferencedColumn(joinColumnField);
    String tblName = getJoinColumnReferencedTable(joinColumnField) + TMP;
    String srcColName = getColumnName(joinColumnField);
    return String.format(JOIN_INNER_SEL_FMT, tblName, refCol, srcTblName + PERIOD + srcColName);
  }
}
