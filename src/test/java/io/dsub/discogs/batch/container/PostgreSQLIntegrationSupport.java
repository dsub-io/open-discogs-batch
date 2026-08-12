package io.dsub.discogs.batch.container;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import io.dsub.discogs.batch.config.CanonicalSchemaMigrator;
import io.dsub.discogs.batch.config.DatabaseSchema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgreSQLIntegrationSupport {

  public static final String TEST_OWNER_LABEL = "io.dsub.test-owner";
  public static final String TEST_OWNER = "open-discogs-batch";
  public static final String TEST_RUN_LABEL = "io.dsub.test-run";
  static final String TEST_RUN_ID_ENVIRONMENT_VARIABLE = "OPEN_DISCOGS_TEST_RUN_ID";
  static final String POSTGRES_DATA_DIRECTORY = "/var/lib/postgresql";
  static final String POSTGRES_TMPFS_OPTIONS = "rw,noexec,nosuid,size=512m";
  static final String TEST_RUN_ID = resolveTestRunId();

  protected static final PostgreSQLContainer CONTAINER;
  protected static final DataSource dataSource;

  static {
    CONTAINER = new PostgreSQLContainer("postgres:18.4-alpine")
        .withDatabaseName("databaseName")
        .withPassword("password")
        .withUsername("username")
        .withLabels(Map.of(TEST_OWNER_LABEL, TEST_OWNER, TEST_RUN_LABEL, TEST_RUN_ID))
        .withTmpFs(Map.of(POSTGRES_DATA_DIRECTORY, POSTGRES_TMPFS_OPTIONS))
        .withReuse(false);
    Runtime.getRuntime()
        .addShutdownHook(new Thread(CONTAINER::stop, "open-discogs-testcontainer-cleanup"));
    try {
      CONTAINER.start();
      verifyContainerConfiguration(CONTAINER.getContainerInfo());
      dataSource = DataSourceBuilder.create()
          .driverClassName(CONTAINER.getDriverClassName())
          .url(CONTAINER.getJdbcUrl())
          .username(CONTAINER.getUsername())
          .password(CONTAINER.getPassword())
          .build();
      new CanonicalSchemaMigrator(
              dataSource, new DatabaseSchema(DatabaseSchema.DEFAULT_NAME))
          .migrate();
    } catch (RuntimeException | Error failure) {
      stopAfterFailure(failure);
      throw failure;
    }
  }

  protected final String jdbcUrl = CONTAINER.getJdbcUrl();
  protected final String password = CONTAINER.getPassword();
  protected final String username = CONTAINER.getUsername();

  private static String resolveTestRunId() {
    String configuredRunId = System.getenv(TEST_RUN_ID_ENVIRONMENT_VARIABLE);
    return configuredRunId == null || configuredRunId.isBlank()
        ? DockerClientFactory.SESSION_ID
        : configuredRunId;
  }

  static void verifyContainerConfiguration(InspectContainerResponse containerInfo) {
    Map<String, String> labels = Objects.requireNonNull(
        Objects.requireNonNull(containerInfo.getConfig()).getLabels());
    requireConfiguration(TEST_OWNER.equals(labels.get(TEST_OWNER_LABEL)),
        "test container owner label is missing");
    requireConfiguration(TEST_RUN_ID.equals(labels.get(TEST_RUN_LABEL)),
        "test container run label is missing");

    HostConfig hostConfig = Objects.requireNonNull(containerInfo.getHostConfig());
    requireConfiguration(
        Map.of(POSTGRES_DATA_DIRECTORY, POSTGRES_TMPFS_OPTIONS).equals(hostConfig.getTmpFs()),
        "PostgreSQL data directory is not the bounded tmpfs");
    requireConfiguration(hostConfig.getBinds() == null || hostConfig.getBinds().length == 0,
        "test container has a persistent bind mount");
    requireConfiguration(hostConfig.getMounts() == null || hostConfig.getMounts().isEmpty(),
        "test container has a persistent configured mount");

    List<InspectContainerResponse.Mount> runtimeMounts = containerInfo.getMounts();
    requireConfiguration(runtimeMounts == null || runtimeMounts.stream().noneMatch(
            mount -> hasText(mount.getName()) || hasText(mount.getSource())),
        "test container has a persistent runtime volume or bind mount");
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static void requireConfiguration(boolean valid, String message) {
    if (!valid) {
      throw new IllegalStateException(message);
    }
  }

  private static void stopAfterFailure(Throwable failure) {
    try {
      CONTAINER.stop();
    } catch (RuntimeException cleanupFailure) {
      failure.addSuppressed(cleanupFailure);
    }
  }

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
