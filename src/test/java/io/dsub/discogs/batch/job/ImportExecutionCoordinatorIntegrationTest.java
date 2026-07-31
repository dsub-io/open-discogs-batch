package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.opendiscogs.model.manifest.ImportManifest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class ImportExecutionCoordinatorIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final LocalDate JULY_DUMP = LocalDate.of(2026, 7, 1);

  @BeforeAll
  static void migrateDatabase() throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet tables =
            connection
                .getMetaData()
                .getTables(null, "public", "discogs_import_run", new String[] {"TABLE"})) {
      if (tables.next()) {
        return;
      }
    }
    ResourceDatabasePopulator migrations =
        new ResourceDatabasePopulator(
            new ClassPathResource("migrations/V001__initial_schema.sql"),
            new ClassPathResource("migrations/V002__discogs_dump_catalog.sql"),
            new ClassPathResource("migrations/V003__discogs_import_history.sql"));
    migrations.execute(dataSource);
  }

  @BeforeEach
  void clearImportHistory() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          truncate table
              discogs_import_run_dump,
              discogs_import_run,
              discogs_dump
          restart identity cascade
          """);
    }
  }

  @Test
  void successfulManifestSkipsByDefaultAndForceCreatesAnotherSuccessfulRun()
      throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters normal = parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false);

    ImportExecutionCoordinator.Preparation first = coordinator.prepare(normal);
    assertThat(first.skipped()).isFalse();
    coordinator.complete(true, null);

    ImportExecutionCoordinator.Preparation skipped = coordinator.prepare(normal);
    assertThat(skipped.skipped()).isTrue();
    assertThat(skipped.priorSuccessfulRunId()).isEqualTo(first.runId());

    JobParameters forced = parameters(EntityType.ARTIST, JULY_DUMP, 'a', true, false);
    ImportExecutionCoordinator.Preparation second = coordinator.prepare(forced);
    assertThat(second.skipped()).isFalse();
    coordinator.complete(true, null);

    assertThat(longQuery("select count(*) from discogs_import_run where status = 'success'"))
        .isEqualTo(2);
    assertThat(stringQuery(
        "select dump_date::text from discogs_import_checkpoint where entity_type = 'artist'"))
        .isEqualTo("2026-07-01");
  }

  @Test
  void overlappingEntityIsRejectedWhileDisjointEntityCanRun() throws Exception {
    ImportExecutionCoordinator artistCoordinator =
        new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator competingCoordinator =
        new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator masterCoordinator =
        new ImportExecutionCoordinator(dataSource);

    artistCoordinator.prepare(parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false));

    assertThatThrownBy(
        () ->
            competingCoordinator.prepare(
                parameters(EntityType.ARTIST, JULY_DUMP.plusMonths(1), 'b', false, false)))
        .hasMessageContaining("already updating artist");

    ImportExecutionCoordinator.Preparation master =
        masterCoordinator.prepare(
            parameters(EntityType.MASTER, JULY_DUMP, 'c', false, false));
    assertThat(master.skipped()).isFalse();

    masterCoordinator.complete(true, null);
    artistCoordinator.complete(true, null);
  }

  @Test
  void olderDumpNeedsSeparateDowngradeOverride() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    coordinator.prepare(parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false));
    coordinator.complete(true, null);

    LocalDate older = JULY_DUMP.minusMonths(1);
    assertThatThrownBy(
        () -> coordinator.prepare(parameters(EntityType.ARTIST, older, 'b', true, false)))
        .hasMessageContaining("predates checkpoint")
        .hasMessageContaining("--allow-downgrade");

    coordinator.prepare(parameters(EntityType.ARTIST, older, 'b', true, true));
    coordinator.complete(true, null);

    assertThat(stringQuery(
        "select dump_date::text from discogs_import_checkpoint where entity_type = 'artist'"))
        .isEqualTo("2026-06-01");
    assertThat(stringQuery(
        """
        select allow_downgrade_requested::text
        from discogs_import_run
        where status = 'success'
        order by id desc
        limit 1
        """))
        .isEqualTo("true");
  }

  @Test
  void failedManifestRemainsRetryable() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parameters(EntityType.LABEL, JULY_DUMP, 'd', false, false);

    coordinator.prepare(parameters);
    coordinator.complete(false, new IllegalStateException("fixture failure"));

    ImportExecutionCoordinator.Preparation retry = coordinator.prepare(parameters);
    assertThat(retry.skipped()).isFalse();
    coordinator.complete(true, null);

    assertThat(longQuery("select count(*) from discogs_import_run where status = 'failed'"))
        .isEqualTo(1);
    assertThat(longQuery("select count(*) from discogs_import_run where status = 'success'"))
        .isEqualTo(1);
  }

  private JobParameters parameters(
      EntityType type,
      LocalDate dumpDate,
      char checksumCharacter,
      boolean force,
      boolean allowDowngrade) {
    String checksum = String.valueOf(checksumCharacter).repeat(64);
    String manifest =
        ImportManifest.fingerprint(
            List.of(new ImportManifest.Dump(type.toString(), dumpDate, checksum)));
    return new JobParametersBuilder()
        .addString(type.toString(), type + "-etag-" + dumpDate)
        .addString(ImportJobParameters.MANIFEST_SHA256, manifest)
        .addString(ImportJobParameters.FORCE, String.valueOf(force))
        .addString(
            ImportJobParameters.ALLOW_DOWNGRADE,
            String.valueOf(allowDowngrade))
        .addString(ImportJobParameters.checksum(type), checksum)
        .addString(ImportJobParameters.date(type), dumpDate.toString())
        .addString(ImportJobParameters.etag(type), type + "-etag-" + dumpDate)
        .addString(ImportJobParameters.size(type), "1024")
        .addString(
            ImportJobParameters.uri(type),
            "data/2026/discogs_"
                + dumpDate.toString().replace("-", "")
                + "_"
                + type
                + "s.xml.gz")
        .toJobParameters();
  }

  private long longQuery(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql)) {
      result.next();
      return result.getLong(1);
    }
  }

  private String stringQuery(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql)) {
      result.next();
      return result.getString(1);
    }
  }
}
