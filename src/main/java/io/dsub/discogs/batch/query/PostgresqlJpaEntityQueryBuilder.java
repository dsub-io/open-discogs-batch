package io.dsub.discogs.batch.query;

import io.dsub.discogs.common.entity.base.BaseEntity;

public class PostgresqlJpaEntityQueryBuilder extends SqlJpaEntityQueryBuilder<BaseEntity> {
  @Override
  public String getSelectInsertQuery(Class<? extends BaseEntity> targetClass) {
    boolean includeId = hasKnownId(targetClass);
    String tblName = getTableName(targetClass);
    String tmpTblName = tblName + TMP;
    String columns = String.join(",",getColumnNames(targetClass));
    StringBuilder sb = new StringBuilder();
    sb.append(INSERT_INTO).append(SPACE).append(tblName)
        .append(OPEN_BRACE)
        .append(String.join(",",getColumnNames(targetClass)))
        .append(CLOSE_BRACE);

    return sb.toString();
  }

  @Override
  public String getPruneQuery(Class<? extends BaseEntity> targetClass) {
    return null;
  }
}
