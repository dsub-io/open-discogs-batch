package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
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
  private static final int CHUNK_SIZE = 5;

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
            new ClassPathResource("migrations/V003__discogs_import_history.sql"),
            new ClassPathResource("migrations/V004__allow_reissued_dump_paths.sql"),
            new ClassPathResource("migrations/V005__durable_import_progress.sql"),
            new ClassPathResource("migrations/V006__concurrent_import_progress.sql"));
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
    completeSuccessfully(coordinator, first.runId());

    ImportExecutionCoordinator.Preparation skipped = coordinator.prepare(normal);
    assertThat(skipped.skipped()).isTrue();
    assertThat(skipped.priorSuccessfulRunId()).isEqualTo(first.runId());

    JobParameters forced = parameters(EntityType.ARTIST, JULY_DUMP, 'a', true, false);
    ImportExecutionCoordinator.Preparation second = coordinator.prepare(forced);
    assertThat(second.skipped()).isFalse();
    completeSuccessfully(coordinator, second.runId());

    assertThat(longQuery("select count(*) from discogs_import_run where status = 'success'"))
        .isEqualTo(2);
    assertThat(stringQuery(
        "select dump_date::text from discogs_import_checkpoint where entity_type = 'artist'"))
        .isEqualTo("2026-07-01");
  }

  @Test
  void sameCoordinatorCannotPrepareAnotherRunUntilCompletion() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters artist = parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false);
    ImportExecutionCoordinator.Preparation preparation = coordinator.prepare(artist);

    assertThatThrownBy(() -> coordinator.prepare(artist))
        .hasMessageContaining("already been prepared");

    coordinator.complete(false, new IllegalStateException("test cleanup"));
    assertThat(preparation.runId()).isPositive();
  }

  @Test
  void completionWithoutPreparationIsANoOp() throws Exception {
    new ImportExecutionCoordinator(dataSource).complete(true, null);
  }

  @Test
  void invalidPlanAndChunkParametersFailBeforeDatabaseAdmission() {
    JobParameters emptyPlan =
        new JobParametersBuilder()
            .addString(ImportJobParameters.MANIFEST_SHA256, "a".repeat(64))
            .addString(ImportJobParameters.CHUNK_SIZE, String.valueOf(CHUNK_SIZE))
            .toJobParameters();
    assertThatThrownBy(
            () -> new ImportExecutionCoordinator(dataSource).prepare(emptyPlan))
        .hasMessageContaining("at least one entity type");

    for (String invalidChunkSize : List.of("0", "-1", "2147483648", "invalid")) {
      JobParameters invalid =
          new JobParametersBuilder(
                  parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false))
              .addString(ImportJobParameters.CHUNK_SIZE, invalidChunkSize)
              .toJobParameters();
      assertThatThrownBy(
              () -> new ImportExecutionCoordinator(dataSource).prepare(invalid))
          .hasMessageContaining("must be a positive integer");
    }
  }

  @Test
  void numericJobParameterFallbackIsAcceptedAndMissingManifestIsRejected() throws Exception {
    JobParameters numericChunk =
        new JobParametersBuilder(parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false))
            .addLong(ImportJobParameters.CHUNK_SIZE, (long) CHUNK_SIZE)
            .toJobParameters();
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation preparation = coordinator.prepare(numericChunk);
    coordinator.complete(false, null);
    assertThat(preparation.runId()).isPositive();

    JobParameters missingManifest =
        new JobParametersBuilder()
            .addString(EntityType.ARTIST.toString(), "artist")
            .addString(ImportJobParameters.CHUNK_SIZE, String.valueOf(CHUNK_SIZE))
            .toJobParameters();
    assertThatThrownBy(
            () -> new ImportExecutionCoordinator(dataSource).prepare(missingManifest))
        .hasMessageContaining("missing import job parameter: import.manifestSha256");
  }

  @Test
  void malformedDumpMetadataFailsBeforeCreatingARun() {
    JobParameters malformed =
        new JobParametersBuilder(
                parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false))
            .addString(ImportJobParameters.date(EntityType.ARTIST), "not-a-date")
            .toJobParameters();

    assertThatThrownBy(
            () -> new ImportExecutionCoordinator(dataSource).prepare(malformed))
        .hasMessageContaining("invalid import parameters for artist");
  }

  @Test
  void databaseAdmissionFailureRollsBackAndReleasesLocks() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters invalidChecksum =
        new JobParametersBuilder(
                parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false))
            .addString(ImportJobParameters.checksum(EntityType.ARTIST), "z".repeat(64))
            .toJobParameters();

    assertThatThrownBy(() -> coordinator.prepare(invalidChecksum))
        .hasMessageContaining("failed to prepare import execution");
    assertThat(longQuery("select count(*) from discogs_import_run")).isZero();

    ImportExecutionCoordinator.Preparation valid =
        coordinator.prepare(parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false));
    coordinator.complete(false, new IllegalStateException("test cleanup"));
    assertThat(valid.runId()).isPositive();
  }

  @Test
  void completionRejectsARunWhoseOwnershipWasExternallyRevoked() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation preparation =
        coordinator.prepare(parameters(EntityType.LABEL, JULY_DUMP, 'a', false, false));
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          update discogs_import_run
          set status = 'failed', completed_at = now()
          where id = %d
          """.formatted(preparation.runId()));
    }

    assertThatThrownBy(
            () -> coordinator.complete(false, new IllegalStateException("late completion")))
        .hasMessageContaining("was not in running state");

    ImportExecutionCoordinator next = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation admitted =
        next.prepare(parameters(EntityType.LABEL, JULY_DUMP, 'b', false, false));
    next.complete(false, new IllegalStateException("test cleanup"));
    assertThat(admitted.runId()).isPositive();
  }

  @Test
  void dependencyLocksRejectUnsafeOverlapWhileIndependentEntityCanRun() throws Exception {
    ImportExecutionCoordinator artistCoordinator =
        new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator competingCoordinator =
        new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator masterCoordinator =
        new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator labelCoordinator =
        new ImportExecutionCoordinator(dataSource);

    artistCoordinator.prepare(parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false));

    assertThatThrownBy(
        () ->
            competingCoordinator.prepare(
                parameters(EntityType.ARTIST, JULY_DUMP.plusMonths(1), 'b', false, false)))
        .hasMessageContaining("already updating artist");

    assertThatThrownBy(
            () ->
                masterCoordinator.prepare(
                    parameters(EntityType.MASTER, JULY_DUMP, 'c', false, false)))
        .hasMessageContaining("already updating artist");

    ImportExecutionCoordinator.Preparation label =
        labelCoordinator.prepare(
            parameters(EntityType.LABEL, JULY_DUMP, 'd', false, false));
    assertThat(label.skipped()).isFalse();

    completeSuccessfully(labelCoordinator, label.runId());
    completeSuccessfully(artistCoordinator, activeRunId(EntityType.ARTIST));
  }

  @Test
  void releaseLocksEveryReferencedEntityAndMasterWriteTarget() throws Exception {
    ImportExecutionCoordinator releaseCoordinator =
        new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator labelCoordinator =
        new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator masterCoordinator =
        new ImportExecutionCoordinator(dataSource);

    ImportExecutionCoordinator.Preparation release =
        releaseCoordinator.prepare(
            parameters(EntityType.RELEASE, JULY_DUMP, 'e', false, false));

    assertThatThrownBy(
            () ->
                labelCoordinator.prepare(
                    parameters(EntityType.LABEL, JULY_DUMP, 'f', false, false)))
        .hasMessageContaining("already updating label");
    assertThatThrownBy(
            () ->
                masterCoordinator.prepare(
                    parameters(EntityType.MASTER, JULY_DUMP, 'a', false, false)))
        .hasMessageContaining("already updating artist");

    completeSuccessfully(releaseCoordinator, release.runId());
  }

  @Test
  void olderDumpNeedsSeparateDowngradeOverride() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    coordinator.prepare(parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false));
    completeSuccessfully(coordinator, activeRunId(EntityType.ARTIST));

    LocalDate older = JULY_DUMP.minusMonths(1);
    assertThatThrownBy(
        () -> coordinator.prepare(parameters(EntityType.ARTIST, older, 'b', true, false)))
        .hasMessageContaining("predates checkpoint")
        .hasMessageContaining("--allow-downgrade");

    coordinator.prepare(parameters(EntityType.ARTIST, older, 'b', true, true));
    completeSuccessfully(coordinator, activeRunId(EntityType.ARTIST));

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
    assertThat(retry.resumedFromRunId()).isNotNull();
    completeSuccessfully(coordinator, retry.runId());

    assertThat(longQuery("select count(*) from discogs_import_run where status = 'failed'"))
        .isEqualTo(1);
    assertThat(longQuery("select count(*) from discogs_import_run where status = 'success'"))
        .isEqualTo(1);
  }

  @Test
  void historicalSuccessDoesNotSkipAfterACompleteNewerSnapshot() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters july = parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false);
    ImportExecutionCoordinator.Preparation julyRun = coordinator.prepare(july);
    completeSuccessfully(coordinator, julyRun.runId());

    JobParameters august =
        parameters(EntityType.ARTIST, JULY_DUMP.plusMonths(1), 'b', false, false);
    ImportExecutionCoordinator.Preparation augustRun = coordinator.prepare(august);
    completeSuccessfully(coordinator, augustRun.runId());

    ImportExecutionCoordinator.Preparation reapplied =
        coordinator.prepare(parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, true));

    assertThat(reapplied.skipped()).isFalse();
    assertThat(reapplied.resumedFromRunId()).isNull();
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void successfulCheckpointDoesNotSkipAfterALaterPartialFailure() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters july = parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false);
    ImportExecutionCoordinator.Preparation julyRun = coordinator.prepare(july);
    completeSuccessfully(coordinator, julyRun.runId());

    JobParameters august =
        parameters(EntityType.ARTIST, JULY_DUMP.plusMonths(1), 'b', false, false);
    ImportExecutionCoordinator.Preparation failed = coordinator.prepare(august);
    recordChunk(failed.runId(), EntityType.ARTIST, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));

    ImportExecutionCoordinator.Preparation retry = coordinator.prepare(july);

    assertThat(retry.skipped()).isFalse();
    assertThat(retry.resumedFromRunId()).isNull();
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void matchingFailureTransfersChunkProgressAtomically() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parameters(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation failed = coordinator.prepare(parameters);
    recordChunk(failed.runId(), EntityType.RELEASE, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));

    ImportExecutionCoordinator.Preparation resumed = coordinator.prepare(parameters);

    assertThat(resumed.resumedFromRunId()).isEqualTo(failed.runId());
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = " + failed.runId()))
        .isZero();
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = " + resumed.runId()))
        .isEqualTo(1);
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void forceStartsFreshInsteadOfReusingFailedProgress() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters normal =
        parameters(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation failed = coordinator.prepare(normal);
    recordChunk(failed.runId(), EntityType.RELEASE, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));

    JobParameters forced =
        parameters(EntityType.RELEASE, JULY_DUMP, 'e', true, false);
    ImportExecutionCoordinator.Preparation fresh = coordinator.prepare(forced);

    assertThat(fresh.resumedFromRunId()).isNull();
    assertThat(
            longQuery(
                "select count(*) from discogs_import_run_chunk where import_run_id = "
                    + fresh.runId()))
        .isZero();
    assertThat(
            longQuery(
                "select count(*) from discogs_import_run_chunk where import_run_id = "
                    + failed.runId()))
        .isEqualTo(1);
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void successfulCompletionRejectsIncompleteEntityProgress() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation preparation =
        coordinator.prepare(parameters(EntityType.MASTER, JULY_DUMP, 'd', false, false));

    assertThatThrownBy(() -> coordinator.complete(true, null))
        .isInstanceOf(ImportExecutionException.class)
        .hasMessageContaining("1 incomplete entities");
    assertThat(stringQuery(
        "select status from discogs_import_run where id = " + preparation.runId()))
        .isEqualTo("failed");
  }

  @Test
  void newerSuccessfulProcessorStateInvalidatesOlderFailedLedger() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters july = parameters(EntityType.LABEL, JULY_DUMP, 'a', false, false);
    ImportExecutionCoordinator.Preparation failed = coordinator.prepare(july);
    recordChunk(failed.runId(), EntityType.LABEL, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));

    JobParameters august =
        parameters(EntityType.LABEL, JULY_DUMP.plusMonths(1), 'b', false, false);
    ImportExecutionCoordinator.Preparation current = coordinator.prepare(august);
    completeSuccessfully(coordinator, current.runId());

    ImportExecutionCoordinator.Preparation retry =
        coordinator.prepare(parameters(EntityType.LABEL, JULY_DUMP, 'a', false, true));

    assertThat(retry.resumedFromRunId()).isNull();
    coordinator.complete(false, new IllegalStateException("test cleanup"));
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
        .addString(ImportJobParameters.CHUNK_SIZE, String.valueOf(CHUNK_SIZE))
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

  private void completeSuccessfully(
      ImportExecutionCoordinator coordinator, long runId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          update discogs_import_run_dump
          set total_items = processed_items,
              total_chunks = 0,
              last_progress_at = now(),
              completed_at = now()
          where import_run_id = %d
          """.formatted(runId));
    }
    coordinator.complete(true, null);
  }

  private void recordChunk(
      long runId,
      EntityType type,
      long chunkIndex,
      long firstItemIndex,
      int itemCount)
      throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          insert into discogs_import_run_chunk
              (import_run_id, entity_type, chunk_index, first_item_index, item_count)
          values (%d, '%s', %d, %d, %d)
          """.formatted(runId, type, chunkIndex, firstItemIndex, itemCount));
      statement.executeUpdate(
          """
          update discogs_import_run_dump
          set processed_items = processed_items + %d,
              last_progress_at = now()
          where import_run_id = %d
            and entity_type = '%s'
          """.formatted(itemCount, runId, type));
    }
  }

  private long activeRunId(EntityType type) throws Exception {
    return longQuery(
        """
        select run_dump.import_run_id
        from discogs_import_run_dump run_dump
        join discogs_import_run import_run on import_run.id = run_dump.import_run_id
        where run_dump.entity_type = '%s'
          and import_run.status = 'running'
        order by import_run.id desc
        limit 1
        """.formatted(type));
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
