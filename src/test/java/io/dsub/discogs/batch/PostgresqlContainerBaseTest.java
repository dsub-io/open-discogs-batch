package io.dsub.discogs.batch;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresqlContainerBaseTest {

    protected static final PostgreSQLContainer postgreSQLContainer;

    final String jdbcUrl = postgreSQLContainer.getJdbcUrl();
    final String password = postgreSQLContainer.getPassword();
    final String username = postgreSQLContainer.getUsername();

    static {
        postgreSQLContainer = new PostgreSQLContainer("postgres:latest")
                .withDatabaseName("databaseName")
                .withPassword("password")
                .withUsername("username");
        postgreSQLContainer.withInitScript("db/postgres/postgres-init.sql");
        postgreSQLContainer.start();
    }

    static class PostgresqlPropertiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of("spring.datasource.driver-class-name=" + postgreSQLContainer.getDriverClassName(),
                            "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                            "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                            "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl())
                    .applyTo(applicationContext.getEnvironment());
        }
    }
}