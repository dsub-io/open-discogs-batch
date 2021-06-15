package io.dsub.discogs.batch.query;

import io.dsub.discogs.common.entity.base.BaseEntity;

public class MySQLJpaEntityQueryBuilder extends SqlJpaEntityQueryBuilder<BaseEntity> {
  @Override
  public String getSelectInsertQuery(Class<? extends BaseEntity> targetClass) {
    return null;
  }

  @Override
  public String getPruneQuery(Class<? extends BaseEntity> targetClass) {
    return null;
  }
}
