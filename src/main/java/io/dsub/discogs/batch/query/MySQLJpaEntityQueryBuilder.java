package io.dsub.discogs.batch.query;

import io.dsub.discogs.common.entity.base.BaseEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Column;

public class MySQLJpaEntityQueryBuilder extends SqlJpaEntityQueryBuilder<BaseEntity> {
  @Override
  public String getSelectInsertQuery(Class<? extends BaseEntity> targetClass) {
    return null;
  }

  @Override
  public String getPruneQuery(Class<? extends BaseEntity> targetClass) {
    return null;
  }

  @Override
  public String getIdOnlyInsertQuery(Class<? extends BaseEntity> targetClass) {
    return null;
  }
}
