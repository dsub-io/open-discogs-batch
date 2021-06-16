package io.dsub.discogs.batch.query;

import io.dsub.discogs.common.entity.base.BaseEntity;
import java.lang.reflect.Field;
import java.util.stream.Collectors;

public class MySQLJpaEntityQueryBuilder extends SqlJpaEntityQueryBuilder<BaseEntity> {

  public static final String INSERT_IGNORE_QUERY = "INSERT IGNORE INTO %s(%s) SELECT %s FROM %s";
  public static final String UPSERT_QUERY =
      "INSERT INTO %s(%s) SELECT %s FROM %s ON DUPLICATE KEY UPDATE %s";
  public static final String UPDATE_PART_WITH_TABLE = "%s=`%s`.`%s`";
  public static final String UPDATE_PART_LAST_MODIFIED = "%s=NOW()";

  @Override
  public String getSelectInsertQuery(Class<? extends BaseEntity> targetClass) {
    // prepare
    String tblName = getTableName(targetClass);
    String tmpTblName = tblName + TMP;
    String columns = String.join(",", getColumnNames(targetClass));

    if (isNoUpdateRequired(targetClass)) {
      return String.format(INSERT_IGNORE_QUERY, tblName, columns, columns, tmpTblName);
    }

    String updateClause = getFormattedUpdateColumns(targetClass);
    return String.format(UPSERT_QUERY, tblName, columns, columns, tmpTblName, updateClause);
  }

  private boolean isNoUpdateRequired(Class<? extends BaseEntity> targetClass) {
    return getUpdateColumns(targetClass).isEmpty();
  }

  private String getFormattedUpdateColumns(Class<? extends BaseEntity> targetClass) {
    Field lastModifiedAt = getLastModifiedAtField(targetClass);
    String lastModifiedColumnName = lastModifiedAt == null ? "" : getColumnName(lastModifiedAt);
    String tmpTblName = getTableName(targetClass);
    return getUpdateColumns(targetClass).stream()
        .map(
            columnName -> {
              if (columnName.equals(lastModifiedColumnName)) {
                return columnName + EQUALS + "NOW()";
              }
              return String.format("%s=%s.%s", columnName, tmpTblName, columnName);
            })
        .collect(Collectors.joining(","));
  }
}
