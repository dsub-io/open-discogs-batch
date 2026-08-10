package io.dsub.discogs.batch.container;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgreSQLIntegrationSupport {

  public static final String TEST_OWNER_LABEL = "io.dsub.test-owner";
  public static final String TEST_OWNER = "open-discogs-batch";
  private static final String POSTGRES_DATA_DIRECTORY = "/var/lib/postgresql";

  protected static final PostgreSQLContainer CONTAINER;
  protected static final DataSource dataSource;

  static {
    CONTAINER = new PostgreSQLContainer("postgres:18.4-alpine")
        .withDatabaseName("databaseName")
        .withPassword("password")
        .withUsername("username")
        .withLabel(TEST_OWNER_LABEL, TEST_OWNER)
        .withTmpFs(Map.of(POSTGRES_DATA_DIRECTORY, "rw"))
        .withReuse(false);
    CONTAINER.start();
    Runtime.getRuntime()
        .addShutdownHook(new Thread(CONTAINER::stop, "open-discogs-testcontainer-cleanup"));
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
