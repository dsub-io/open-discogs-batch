package io.dsub.discogs.batch.query;

import io.dsub.discogs.common.entity.base.BaseEntity;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class PostgresqlJpaEntityQueryBuilder extends SqlJpaEntityQueryBuilder<BaseEntity> {

  public static final String EXCLUDED = "excluded";

  public static final String SELECT_INSERT_QUERY_PATTERN =
      "INSERT INTO %s(%s) SELECT %s FROM %s ON CONFLICT (%s) DO UPDATE SET %s";

  @Override
  public String getSelectInsertQuery(Class<? extends BaseEntity> targetClass) {
    String tblName = getTableName(targetClass);
    String tmpTblName = tblName + TMP;
    String columns = String.join(",", getColumnNames(targetClass));
    String constraintColumns = String.join(",", getConstraintColumns(targetClass));
    String updateClause = mapDoUpdateClause(targetClass);

    return String.format(
        SELECT_INSERT_QUERY_PATTERN,
        tblName,
        columns,
        columns,
        tmpTblName,
        constraintColumns,
        updateClause);
  }

  private String mapDoUpdateClause(Class<? extends BaseEntity> targetClass) {
    Field lastModField = getLastModifiedAtField(targetClass);
    List<String> columnsToUpdate = getUpdateColumns(targetClass);
    String lastModCol = lastModField != null ? getColumnName(lastModField) : null;

    return OPEN_BRACE
        + String.join(",", columnsToUpdate)
        + CLOSE_BRACE
        + EQUALS
        + OPEN_BRACE
        + columnsToUpdate.stream()
            .map(col -> EXCLUDED + PERIOD + col)
            .map(col -> normalizeUpdateValueColumn(lastModCol, col))
            .collect(Collectors.joining(","))
        + CLOSE_BRACE;
  }

  private String normalizeUpdateValueColumn(String lastModCol, String col) {
    if (lastModCol != null && !lastModCol.isBlank() && col.contains(lastModCol)) {
      return "NOW()";
    }
    return col;
  }
}
