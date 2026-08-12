package io.dsub.discogs.batch;

import io.dsub.discogs.batch.config.CanonicalSchemaMigrator;
import io.dsub.discogs.batch.config.DatabaseSchema;
import io.dsub.discogs.batch.config.DatabaseSchemaProvisioner;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures the canonical schema migration boundary. */
@Slf4j
@Configuration
public class LiquibaseConfig {

  private static final String PUBLIC_CHANGELOG =
      "classpath:db/changelog/db.changelog-master.xml";
  private static final String CUSTOM_SCHEMA_CHANGELOG =
      "classpath:db/changelog/db.changelog-custom-schema.xml";
  private static final String PUBLIC_SCHEMA_WARNING =
      "database schema is public; set --database-schema or OPEN_DISCOGS_BATCH_DATABASE_SCHEMA to isolate OpenDiscogs tables";

  @Bean
  public DatabaseSchema databaseSchema(ApplicationArguments arguments) {
    DatabaseSchema schema = DatabaseSchema.from(arguments);
    if (schema.isPublic()) {
      log.warn(PUBLIC_SCHEMA_WARNING);
    }
    return schema;
  }

  @Bean
  public DatabaseSchemaProvisioner databaseSchemaProvisioner(
      DataSource dataSource, DatabaseSchema schema) {
    return new DatabaseSchemaProvisioner(dataSource, schema);
  }

  @Bean
  public CanonicalSchemaMigrator canonicalSchemaMigrator(
      DataSource dataSource, DatabaseSchema schema) {
    return new CanonicalSchemaMigrator(dataSource, schema);
  }

  @Bean
  public SpringLiquibase liquibase(
      DataSource dataSource,
      DatabaseSchema schema,
      DatabaseSchemaProvisioner provisioner,
      CanonicalSchemaMigrator migrator) {
    boolean created = provisioner.ensure();
    migrator.migrate();
    log.info("database schema ready: {} (created={})", schema.name(), created);
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setChangeLog(schema.isPublic() ? PUBLIC_CHANGELOG : CUSTOM_SCHEMA_CHANGELOG);
    liquibase.setShouldRun(true);
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema(schema.name());
    liquibase.setLiquibaseSchema(schema.name());
    liquibase.setChangeLogParameters(Map.of("databaseSchema", schema.name()));
    return liquibase;
  }
}
