package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.datasource.DBType;
import io.dsub.discogsdata.batch.datasource.DataSourceConfig;
import io.dsub.discogsdata.batch.query.JpaEntityQueryBuilder;
import io.dsub.discogsdata.batch.query.MySQLJpaEntityQueryBuilder;
import io.dsub.discogsdata.batch.query.PostgresqlJpaEntityQueryBuilder;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Order(1)
@Configuration
public class QueryBuilderConfig {

  @Bean
  public JpaEntityQueryBuilder<BaseEntity> queryBuilder() {
    DBType dbType = DataSourceConfig.getDB_TYPE();
    if (dbType.equals(DBType.POSTGRESQL)) {
      return new PostgresqlJpaEntityQueryBuilder();
    } else if (dbType.equals(DBType.MYSQL)) {
      return new MySQLJpaEntityQueryBuilder();
    }
    throw new UnsupportedOperationException("unsupported DBType: " + dbType);
  }
}
