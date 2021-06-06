package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.datasource.DBType;
import io.dsub.discogsdata.batch.datasource.DataSourceProperties;
import io.dsub.discogsdata.batch.query.MySQLJpaEntityQueryBuilder;
import io.dsub.discogsdata.batch.query.PostgresqlJpaEntityQueryBuilder;
import io.dsub.discogsdata.batch.query.QueryBuilder;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Order(1)
@Configuration
@RequiredArgsConstructor
public class QueryBuilderConfig {

  private final DataSourceProperties properties;

  @Bean
  public QueryBuilder<BaseEntity> queryBuilder() {
    DBType dbType = properties.getDbType();
    if (dbType.equals(DBType.POSTGRESQL)) {
      return new PostgresqlJpaEntityQueryBuilder();
    } else if (dbType.equals(DBType.MYSQL)) {
      return new MySQLJpaEntityQueryBuilder();
    }
    // in case of future additions for types.
    throw new UnsupportedOperationException("unsupported DBType: " + dbType);
  }
}
