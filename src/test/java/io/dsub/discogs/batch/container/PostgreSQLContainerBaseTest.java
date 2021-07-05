package io.dsub.discogs.batch.container;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgreSQLContainerBaseTest {

  protected static final PostgreSQLContainer CONTAINER;
  protected static final DataSource dataSource;

  static {
    CONTAINER = new PostgreSQLContainer("postgres:latest")
        .withDatabaseName("databaseName")
        .withPassword("password")
        .withUsername("username");
    CONTAINER.start();
    dataSource = DataSourceBuilder.create()
        .driverClassName(CONTAINER.getDriverClassName())
        .url(CONTAINER.getJdbcUrl())
        .username(CONTAINER.getUsername())
        .password(CONTAINER.getPassword())
        .build();
  }

  protected final String jdbcUrl = CONTAINER.getJdbcUrl();
  protected final String password = CONTAINER.getPassword();
  protected final String username = CONTAINER.getUsername();

  static class PostgreSQLPropertiesInitializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertyValues.of("spring.datasource.driver-class-name=" + CONTAINER.getDriverClassName(),
          "spring.datasource.username=" + CONTAINER.getUsername(),
          "spring.datasource.password=" + CONTAINER.getPassword(),
          "spring.datasource.url=" + CONTAINER.getJdbcUrl())
          .applyTo(applicationContext.getEnvironment());
    }
  }
}