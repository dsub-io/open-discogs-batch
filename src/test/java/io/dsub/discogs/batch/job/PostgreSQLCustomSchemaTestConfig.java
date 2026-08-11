package io.dsub.discogs.batch.job;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.config.DatabaseSchema;
import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgreSQLCustomSchemaTestConfig extends PostgreSQLIntegrationSupport {

  static final String SCHEMA_NAME = "open_discogs";

  @Bean(destroyMethod = "close")
  public DataSource dataSource() {
    HikariDataSource customDataSource = new HikariDataSource();
    customDataSource.setDriverClassName(CONTAINER.getDriverClassName());
    customDataSource.setJdbcUrl(CONTAINER.getJdbcUrl());
    customDataSource.setUsername(CONTAINER.getUsername());
    customDataSource.setPassword(CONTAINER.getPassword());
    customDataSource.setConnectionInitSql(
        new DatabaseSchema(SCHEMA_NAME).connectionInitializationSql());
    return customDataSource;
  }

  @Bean
  public ApplicationArguments applicationArguments() {
    return new DefaultApplicationArguments("--databaseSchema=" + SCHEMA_NAME);
  }
}
