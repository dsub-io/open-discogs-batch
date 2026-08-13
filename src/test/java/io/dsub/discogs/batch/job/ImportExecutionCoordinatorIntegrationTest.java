package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
import io.dsub.opendiscogs.model.manifest.ImportExecution;
import io.dsub.opendiscogs.model.manifest.ImportManifest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

class ImportExecutionCoordinatorIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final LocalDate JULY_DUMP = LocalDate.of(2026, 7, 1);
  private static final int CHUNK_SIZE = 5;
  private static final int LEGACY_IMPORT_CONTRACT_REVISION = 1;
  private static final String GO_PROCESSOR = "go-open-discogs-batch";
  private static final String GO_PROCESSOR_VERSION = "2.3.6";

  @BeforeAll
  static void migrateDatabase() throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet tables =
            connection
                .getMetaData()
                .getTables(null, "public", "discogs_import_run", new String[] {"TABLE"})) {
      if (tables.next()) {
        addImportContractRevisionFixture();
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
    addImportContractRevisionFixture();
  }

  private static void addImportContractRevisionFixture() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          alter table discogs_import_run_dump
          add column if not exists import_contract_revision integer not null default 1
          """);
    }
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
  void partialReleaseRequiresCompletedDependencyCheckpoints() {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);

    assertThatThrownBy(
            () ->
                coordinator.prepare(
                    parameters(EntityType.RELEASE, JULY_DUMP, 'e', false, false)))
        .isInstanceOf(ImportExecutionException.class)
        .hasMessageContaining("completed artist checkpoint")
        .hasMessageContaining("release");
  }

  @Test
  void partialReleaseReusesCompletedDependenciesFromAFailedRun() throws Exception {
    ImportExecutionCoordinator dependencies = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation completed =
        dependencies.prepare(
            parameters(
                List.of(
                    new SelectedDump(EntityType.ARTIST, JULY_DUMP, 'a'),
                    new SelectedDump(EntityType.LABEL, JULY_DUMP, 'b'),
                    new SelectedDump(EntityType.MASTER, JULY_DUMP, 'c')),
                false,
                false));
    completeSuccessfully(dependencies, completed.runId());
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          update discogs_import_run
          set status = 'failed', failure_message = 'release failed after dependencies completed'
          where id = %d
          """.formatted(completed.runId()));
    }

    ImportExecutionCoordinator release = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation preparation =
        release.prepare(parameters(EntityType.RELEASE, JULY_DUMP, 'd', false, false));

    assertThat(preparation.skipped()).isFalse();
    release.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void dependencyCheckpointAtTheNextMonthBoundaryIsCompatible() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation artist =
        coordinator.prepare(
            parameters(EntityType.ARTIST, JULY_DUMP.plusMonths(1), 'a', false, false));
    completeSuccessfully(coordinator, artist.runId());

    ImportExecutionCoordinator.Preparation master =
        coordinator.prepare(parameters(EntityType.MASTER, JULY_DUMP, 'c', false, false));

    assertThat(master.skipped()).isFalse();
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void successfulManifestSkipsByDefaultAndForceCreatesAnotherSuccessfulRun()
      throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters normal = parameters(EntityType.ARTIST, JULY_DUMP, 'a', false, false);

    ImportExecutionCoordinator.Preparation first = coordinator.prepare(normal);
    assertThat(first.skipped()).isFalse();
    assertThat(importContractRevision(first.runId(), EntityType.ARTIST))
        .isEqualTo(currentImportContractRevision(EntityType.ARTIST));
    completeSuccessfully(coordinator, first.runId());

    ImportExecutionCoordinator.Preparation skipped = coordinator.prepare(normal);
    assertThat(skipped.skipped()).isTrue();
    assertThat(skipped.priorSuccessfulRunId()).isEqualTo(first.runId());

    JobParameters forced = parameters(EntityType.ARTIST, JULY_DUMP, 'a', true, false);
    ImportExecutionCoordinator.Preparation second = coordinator.prepare(forced);
    assertThat(second.skipped()).isFalse();
    assertThat(importContractRevision(second.runId(), EntityType.ARTIST))
        .isEqualTo(currentImportContractRevision(EntityType.ARTIST));
    completeSuccessfully(coordinator, second.runId());

    assertThat(longQuery("select count(*) from discogs_import_run where status = 'success'"))
        .isEqualTo(2);
    assertThat(stringQuery(
        "select dump_date::text from discogs_import_checkpoint where entity_type = 'artist'"))
        .isEqualTo("2026-07-01");
  }

  @Test
  void legacyReleaseSuccessDoesNotSkipAndIsReprocessedAtTheCurrentRevision()
      throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation legacy = coordinator.prepare(parameters);
    completeSuccessfully(coordinator, legacy.runId());
    setRunDumpRevision(legacy.runId(), EntityType.RELEASE, LEGACY_IMPORT_CONTRACT_REVISION);
    setRunProcessor(legacy.runId(), ImportExecutionCoordinator.PROCESSOR, "1.2.1");

    ImportExecutionCoordinator.Preparation current = coordinator.prepare(parameters);

    assertThat(current.skipped()).isFalse();
    assertThat(current.resumedFromRunId()).isNull();
    assertThat(importContractRevision(current.runId(), EntityType.RELEASE))
        .isEqualTo(currentImportContractRevision(EntityType.RELEASE));
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void legacyReleaseRevisionMakesTheWholeSelectedManifestRunFresh() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters allEntities = allEntityParameters(JULY_DUMP, false, false);
    ImportExecutionCoordinator.Preparation legacy = coordinator.prepare(allEntities);
    completeSuccessfully(coordinator, legacy.runId());

    ImportExecutionCoordinator.Preparation currentMap = coordinator.prepare(allEntities);
    assertThat(currentMap.skipped()).isTrue();
    assertThat(currentMap.priorSuccessfulRunId()).isEqualTo(legacy.runId());

    setRunDumpRevision(legacy.runId(), EntityType.RELEASE, LEGACY_IMPORT_CONTRACT_REVISION);

    ImportExecutionCoordinator.Preparation fresh = coordinator.prepare(allEntities);

    assertThat(fresh.skipped()).isFalse();
    assertThat(fresh.resumedFromRunId()).isNull();
    assertThat(longQuery(
        "select count(*) from discogs_import_run_dump where import_run_id = "
            + fresh.runId())).isEqualTo(EntityType.values().length);
    for (EntityType entityType : EntityType.values()) {
      assertThat(importContractRevision(fresh.runId(), entityType))
          .isEqualTo(currentImportContractRevision(entityType));
    }
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void currentSuccessfulRunSkipsAcrossProcessorImplementationsAndVersions()
      throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters = parameters(EntityType.LABEL, JULY_DUMP, 'b', false, false);
    ImportExecutionCoordinator.Preparation goRun = coordinator.prepare(parameters);
    completeSuccessfully(coordinator, goRun.runId());
    setRunProcessor(goRun.runId(), GO_PROCESSOR, GO_PROCESSOR_VERSION);

    ImportExecutionCoordinator.Preparation skipped = coordinator.prepare(parameters);

    assertThat(skipped.skipped()).isTrue();
    assertThat(skipped.priorSuccessfulRunId()).isEqualTo(goRun.runId());
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
            parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false));

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
    assertThat(importContractRevision(activeRunId(EntityType.ARTIST), EntityType.ARTIST))
        .isEqualTo(currentImportContractRevision(EntityType.ARTIST));
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
  void matchingJavaFailureTransfersChunkProgressAtomically() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
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
  void currentGoFailureResumesInJava() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation goRun = coordinator.prepare(parameters);
    recordChunk(goRun.runId(), EntityType.RELEASE, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));
    setRunProcessor(goRun.runId(), GO_PROCESSOR, GO_PROCESSOR_VERSION);

    ImportExecutionCoordinator.Preparation javaRun = coordinator.prepare(parameters);

    assertThat(javaRun.resumedFromRunId()).isEqualTo(goRun.runId());
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = "
            + goRun.runId())).isZero();
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = "
            + javaRun.runId())).isEqualTo(1);
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void compatibleJavaVersionFailureResumes() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation oldVersion = coordinator.prepare(parameters);
    recordChunk(oldVersion.runId(), EntityType.RELEASE, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));
    setRunProcessor(oldVersion.runId(), ImportExecutionCoordinator.PROCESSOR, "1.2.1");

    ImportExecutionCoordinator.Preparation currentVersion = coordinator.prepare(parameters);

    assertThat(currentVersion.resumedFromRunId()).isEqualTo(oldVersion.runId());
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = "
            + oldVersion.runId())).isZero();
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = "
            + currentVersion.runId())).isEqualTo(1);
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void legacyFailedRunNeverResumesEvenWhenManifestAndChunkShapeMatch()
      throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation legacy = coordinator.prepare(parameters);
    recordChunk(legacy.runId(), EntityType.RELEASE, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));
    setRunDumpRevision(legacy.runId(), EntityType.RELEASE, LEGACY_IMPORT_CONTRACT_REVISION);

    ImportExecutionCoordinator.Preparation current = coordinator.prepare(parameters);

    assertThat(current.resumedFromRunId()).isNull();
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = "
            + legacy.runId())).isEqualTo(1);
    assertThat(importContractRevision(current.runId(), EntityType.RELEASE))
        .isEqualTo(currentImportContractRevision(EntityType.RELEASE));
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void currentJavaAbandonedRunResumesWithTheSameVersion() throws Exception {
    ImportExecutionCoordinator interrupted = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parametersWithDependencies(EntityType.MASTER, JULY_DUMP, 'c', false, false);
    ImportExecutionCoordinator.Preparation abandoned = interrupted.prepare(parameters);
    recordChunk(abandoned.runId(), EntityType.MASTER, 0, 0, CHUNK_SIZE);
    ReflectionTestUtils.invokeMethod(interrupted, "releaseEntityLocks");

    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation resumed = coordinator.prepare(parameters);

    assertThat(resumed.resumedFromRunId()).isEqualTo(abandoned.runId());
    assertThat(stringQuery(
        "select status from discogs_import_run where id = " + abandoned.runId()))
        .isEqualTo("failed");
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void legacyAbandonedRunIsFailedButNeverResumed() throws Exception {
    ImportExecutionCoordinator interrupted = new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation abandoned = interrupted.prepare(parameters);
    recordChunk(abandoned.runId(), EntityType.RELEASE, 0, 0, CHUNK_SIZE);
    setRunDumpRevision(
        abandoned.runId(), EntityType.RELEASE, LEGACY_IMPORT_CONTRACT_REVISION);
    ReflectionTestUtils.invokeMethod(interrupted, "releaseEntityLocks");

    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation fresh = coordinator.prepare(parameters);

    assertThat(fresh.resumedFromRunId()).isNull();
    assertThat(stringQuery(
        "select status from discogs_import_run where id = " + abandoned.runId()))
        .isEqualTo("failed");
    assertThat(longQuery(
        "select count(*) from discogs_import_run_chunk where import_run_id = "
            + abandoned.runId())).isEqualTo(1);
    coordinator.complete(false, new IllegalStateException("test cleanup"));
  }

  @Test
  void forceStartsFreshInsteadOfReusingFailedProgress() throws Exception {
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    JobParameters normal =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', false, false);
    ImportExecutionCoordinator.Preparation failed = coordinator.prepare(normal);
    recordChunk(failed.runId(), EntityType.RELEASE, 0, 0, CHUNK_SIZE);
    coordinator.complete(false, new IllegalStateException("interrupted"));

    JobParameters forced =
        parametersWithDependencies(EntityType.RELEASE, JULY_DUMP, 'e', true, false);
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
        coordinator.prepare(
            parametersWithDependencies(EntityType.MASTER, JULY_DUMP, 'd', false, false));

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
    return parameters(
        List.of(new SelectedDump(type, dumpDate, checksumCharacter)), force, allowDowngrade);
  }

  private JobParameters parametersWithDependencies(
      EntityType type,
      LocalDate dumpDate,
      char checksumCharacter,
      boolean force,
      boolean allowDowngrade)
      throws Exception {
    List<SelectedDump> dependencies =
        switch (type) {
          case ARTIST, LABEL -> List.of();
          case MASTER -> List.of(new SelectedDump(EntityType.ARTIST, dumpDate, 'a'));
          case RELEASE ->
              List.of(
                  new SelectedDump(EntityType.ARTIST, dumpDate, 'a'),
                  new SelectedDump(EntityType.LABEL, dumpDate, 'b'),
                  new SelectedDump(EntityType.MASTER, dumpDate, 'c'));
        };
    if (!dependencies.isEmpty()
        && longQuery(
                "select count(*) from discogs_import_checkpoint where entity_type in ("
                    + dependencies.stream()
                        .map(dependency -> "'" + dependency.entityType() + "'")
                        .reduce((left, right) -> left + "," + right)
                        .orElseThrow()
                    + ")")
            != dependencies.size()) {
      ImportExecutionCoordinator dependencyCoordinator =
          new ImportExecutionCoordinator(dataSource);
      ImportExecutionCoordinator.Preparation dependencyPreparation =
          dependencyCoordinator.prepare(parameters(dependencies, false, false));
      completeSuccessfully(dependencyCoordinator, dependencyPreparation.runId());
    }
    return parameters(type, dumpDate, checksumCharacter, force, allowDowngrade);
  }

  private JobParameters allEntityParameters(
      LocalDate dumpDate, boolean force, boolean allowDowngrade) {
    List<SelectedDump> selectedDumps = new ArrayList<>(EntityType.values().length);
    for (EntityType entityType : EntityType.values()) {
      selectedDumps.add(
          new SelectedDump(
              entityType,
              dumpDate,
              (char) ('a' + entityType.ordinal())));
    }
    return parameters(selectedDumps, force, allowDowngrade);
  }

  private JobParameters parameters(
      List<SelectedDump> selectedDumps, boolean force, boolean allowDowngrade) {
    List<ImportManifest.Dump> manifestDumps = new ArrayList<>(selectedDumps.size());
    JobParametersBuilder parameters =
        new JobParametersBuilder()
        .addString(ImportJobParameters.CHUNK_SIZE, String.valueOf(CHUNK_SIZE))
        .addString(ImportJobParameters.FORCE, String.valueOf(force))
        .addString(
            ImportJobParameters.ALLOW_DOWNGRADE,
            String.valueOf(allowDowngrade));
    for (SelectedDump selectedDump : selectedDumps) {
      EntityType type = selectedDump.entityType();
      LocalDate dumpDate = selectedDump.dumpDate();
      String checksum = Character.toString(selectedDump.checksumCharacter()).repeat(64);
      manifestDumps.add(new ImportManifest.Dump(type.toString(), dumpDate, checksum));
      parameters
          .addString(type.toString(), type + "-etag-" + dumpDate)
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
                  + "s.xml.gz");
    }
    return parameters
        .addString(
            ImportJobParameters.MANIFEST_SHA256,
            ImportManifest.fingerprint(manifestDumps))
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

  private int importContractRevision(long runId, EntityType entityType) throws Exception {
    return Math.toIntExact(
        longQuery(
            "select import_contract_revision from discogs_import_run_dump where import_run_id = "
                + runId
                + " and entity_type = '"
                + entityType
                + "'"));
  }

  private int currentImportContractRevision(EntityType entityType) {
    return ImportExecution.importContractRevision(entityType.toString());
  }

  private void setRunDumpRevision(long runId, EntityType entityType, int revision)
      throws Exception {
    try (Connection connection = dataSource.getConnection();
        java.sql.PreparedStatement statement =
            connection.prepareStatement(
                """
                update discogs_import_run_dump
                set import_contract_revision = ?
                where import_run_id = ? and entity_type = ?
                """)) {
      statement.setInt(1, revision);
      statement.setLong(2, runId);
      statement.setString(3, entityType.toString());
      assertThat(statement.executeUpdate()).isEqualTo(1);
    }
  }

  private void setRunProcessor(long runId, String processor, String processorVersion)
      throws Exception {
    try (Connection connection = dataSource.getConnection();
        java.sql.PreparedStatement statement =
            connection.prepareStatement(
                """
                update discogs_import_run
                set processor = ?, processor_version = ?
                where id = ?
                """)) {
      statement.setString(1, processor);
      statement.setString(2, processorVersion);
      statement.setLong(3, runId);
      assertThat(statement.executeUpdate()).isEqualTo(1);
    }
  }

  private record SelectedDump(
      EntityType entityType, LocalDate dumpDate, char checksumCharacter) {
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
