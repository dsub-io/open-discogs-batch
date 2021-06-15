package io.dsub.discogs.batch.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SqlJpaEntityQueryBuilder<T> implements JpaEntityQueryBuilder<T> {

  public static final String DEFAULT_SQL_INSERT_QUERY_FORMAT = "INSERT INTO %s(%s) SELECT %s";

  public static final String WHERE_CLAUSE_QUERY_FORMAT = "WHERE %s = %s";

  public static final String WHERE_CLAUSE_QUERY_INNER_SELECT_FORMAT =
      "(SELECT 1 FROM %s WHERE %s = %s)";

  @Override
  public String getTemporaryInsertQuery(Class<? extends T> targetClass) {
    boolean idInclusive = hasKnownId(targetClass);
    List<Field> fields = getMappedFields(targetClass);
    String tblName = getTableName(targetClass) + TMP;
    Field createdAt = getCreatedAtField(targetClass);
    Field lastModified = getLastModifiedField(targetClass);
    List<String> columns = fields.stream().map(this::getColumnName).collect(Collectors.toList());
    List<String> values =
        fields.stream().map(field -> COLON + field.getName()).collect(Collectors.toList());
    String mappedValues = getFormattedValueFields(createdAt, lastModified, values);
    return String.format(
        DEFAULT_SQL_INSERT_QUERY_FORMAT, tblName, String.join(",", columns), mappedValues);
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
    List<Field> joinColumnFields =
        getMappedFields(targetClass).stream().filter(this::isColumn).collect(Collectors.toList());

    List<String> innerSelects = new ArrayList<>();

    for (Field field : joinColumnFields) {
      String refCol = getJoinColumnReferencedColumn(field);
      String tblName = getJoinColumnReferencedTable(field) + TMP;
      String fieldName = field.getName();
      String sel =
          String.format(WHERE_CLAUSE_QUERY_INNER_SELECT_FORMAT, tblName, refCol, COLON + fieldName);
      innerSelects.add(sel);
    }

    int count = innerSelects.size();
    return String.format(
        WHERE_CLAUSE_QUERY_FORMAT, String.join(SPACE + PLUS + SPACE, innerSelects), count);
  }
}
