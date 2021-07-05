package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.container.PostgreSQLContainerBaseTest;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgreSQLIntegrationTestConfig extends PostgreSQLContainerBaseTest {

  @Bean
  public DataSource dataSource() {
    return dataSource;
  }

  @Bean
  public ApplicationArguments applicationArguments() {
    return new DefaultApplicationArguments(jdbcUrl, username, password);
  }
}
