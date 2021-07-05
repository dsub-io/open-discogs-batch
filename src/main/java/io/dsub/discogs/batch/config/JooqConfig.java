package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.datasource.DataSourceDetails;
import io.dsub.discogs.batch.util.DataSourceUtil;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JooqConfig {

  private final DataSource dataSource;

  @Bean
  public DSLContext dslContext() {
    DataSourceDetails details = dataSourceDetails();
    return DSL.using(dataSource, details.dialect());
  }

  @Bean
  public DataSourceDetails dataSourceDetails() {
    return DataSourceUtil.getDataSourceDetails(dataSource);
  }
}
