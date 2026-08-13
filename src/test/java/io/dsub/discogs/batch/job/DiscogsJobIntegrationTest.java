package io.dsub.discogs.batch.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.LiquibaseConfig;
import io.dsub.discogs.batch.TestDumpGenerator;
import io.dsub.discogs.batch.config.BatchConfig;
import io.dsub.discogs.batch.config.JooqConfig;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import io.dsub.opendiscogs.model.manifest.ImportExecution;
import io.dsub.opendiscogs.model.manifest.ImportManifest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base, Abstract test class for tests based on each types. An inherited integration test for each
 * DB Type MUST provide following beans:
 *
 * <ul>
 *   <li>{@link DataSource}</li>
 *   <li>{@link org.springframework.boot.ApplicationArguments} with database user</li>
 * </ul>
 */
@Slf4j
@SpringBatchTest
@ContextConfiguration(
    classes = {
        BatchConfig.class,
        BatchInfrastructureConfig.class,
        JooqConfig.class,
        LiquibaseConfig.class,
        DiscogsJobIntegrationTestConfig.class
    })
public abstract class DiscogsJobIntegrationTest {

  private static final AtomicLong TEST_MANIFEST_SEQUENCE = new AtomicLong();

  @Autowired
  JobOperatorTestUtils jobOperatorTestUtils;

  @Autowired
  JobOperator jobOperator;

  @Autowired
  JobRepositoryTestUtils jobRepositoryTestUtils;

  @Autowired
  FileUtil fileUtil;
  @Autowired
  Map<EntityType, File> dumpFiles;
  @Autowired
  CountDownLatch exitLatch;
  @Autowired
  DataSource dataSource;
  @RegisterExtension
  LogSpy logSpy = new LogSpy();
  @Autowired
  private Map<EntityType, DiscogsDump> dumpMap;

  @AfterAll
  static void afterAll() throws FileException {
    FileUtil fileUtil =
        SimpleFileUtil.builder().appDirectory("discogs-data-batch-test").isTemporary(false).build();
    fileUtil.clearAll();
  }

  @AfterEach
  public void cleanUp() throws FileException, IOException {
    jobRepositoryTestUtils.removeJobExecutions();
    dumpMap.clear();
    dumpFiles = new TestDumpGenerator(fileUtil.getAppDirectory(true)).createDiscogsDumpFiles();
    clearInvocations(exitLatch);
  }

  private JobParameters defaultJobParameters() {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString("artist", "artist");
    paramsBuilder.addString("label", "label");
    paramsBuilder.addString("master", "master");
    paramsBuilder.addString("release", "release");
    paramsBuilder.addString("chunkSize", "1000");
    return paramsBuilder.toJobParameters();
  }

  final void runAllTypesScenario() throws Exception {
    JobParameters params =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(defaultJobParameters())
            .toJobParameters();
    params = withTrackedImportRun(params);

    JobExecution jobExecution = jobOperator.start(jobOperatorTestUtils.getJob(), params);
    ExitStatus exitStatus = jobExecution.getExitStatus();

    verify(exitLatch, times(1)).countDown();

    assertThat(exitStatus.getExitCode(), is("COMPLETED"));
    assertThat(dumpMap.size(), is(4));

    for (DiscogsDump dump : dumpMap.values()) {
      Path filePath = fileUtil.getFilePath(dump.getFileName(), false);
      assertThat(Files.exists(filePath), is(true));
    }
  }

  final void runSelectiveEntitiesScenario() throws Exception {
    JobParametersBuilder builder = new JobParametersBuilder();
    //    builder.addString("artist", "artist");
    builder.addString("label", "label");
    builder.addString("chunkSize", "1000");
    JobParameters params =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(builder.toJobParameters())
            .toJobParameters();
    params = withTrackedImportRun(params);

    JobExecution jobExecution = jobOperator.start(jobOperatorTestUtils.getJob(), params);
    ExitStatus exitStatus = jobExecution.getExitStatus();

    List<String> logs = logSpy.getLogsByExactLevelAsString(Level.INFO, true, "io.dsub.discogs");

    assertThat(exitStatus.getExitCode(), is("COMPLETED"));
    assertThat(dumpMap.size(), is(1));
    assertThat(
        logs,
        hasItems(
            "artist eTag not found. skipping artist step.",
            "label eTag found. executing label step.",
            "master eTag not found. skipping master step.",
            "release eTag not found. skipping release step."));
  }

  final void runRelationFailurePropagationScenario() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          create or replace function reject_label_progress() returns trigger language plpgsql as $$
          begin
            if new.entity_type = 'label' then
              raise exception 'rejected label progress';
            end if;
            return new;
          end
          $$
          """);
      statement.execute(
          """
          create trigger reject_label_progress
          before insert on discogs_import_run_chunk
          for each row execute function reject_label_progress()
          """);
    }

    try {
      JobParameters selected =
          new JobParametersBuilder()
              .addString("label", "label")
              .addString(ImportJobParameters.CHUNK_SIZE, "1000")
              .toJobParameters();
      JobParameters parameters =
          withTrackedImportRun(
              jobOperatorTestUtils
                  .getUniqueJobParametersBuilder()
                  .addJobParameters(selected)
                  .toJobParameters());

      JobExecution execution = jobOperator.start(jobOperatorTestUtils.getJob(), parameters);

      String stepStatuses =
          execution.getStepExecutions().stream()
              .map(step -> step.getStepName() + "=" + step.getStatus() + "/" + step.getExitStatus())
              .sorted()
              .collect(java.util.stream.Collectors.joining(", "));
      assertThat(stepStatuses, execution.getStatus(), is(BatchStatus.FAILED));
      assertThat(execution.getExitStatus().getExitCode(), is("FAILED"));
    } finally {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()) {
        statement.execute(
            "drop trigger if exists reject_label_progress on discogs_import_run_chunk");
        statement.execute("drop function if exists reject_label_progress()");
      }
    }
  }

  final void runMultiEntityResumeScenario()
      throws Exception {
    ImportExecutionCoordinator importExecutionCoordinator =
        new ImportExecutionCoordinator(dataSource);
    JobParameters parameters = coordinatedParameters(1, EntityType.ARTIST, EntityType.LABEL);
    ImportExecutionCoordinator.Preparation firstPreparation =
        importExecutionCoordinator.prepare(parameters);

    createProgressFailureTrigger();
    JobExecution firstExecution;
    try {
      firstExecution =
          jobOperator.start(
              jobOperatorTestUtils.getJob(),
              withPreparation(parameters, firstPreparation));
    } finally {
      dropProgressFailureTrigger();
    }

    assertThat(firstExecution.getStatus(), is(BatchStatus.FAILED));
    assertThat(
        scalarLong(
            """
            select count(*)
            from discogs_import_run_chunk
            where import_run_id = ?
              and entity_type = 'artist'
            """,
            firstPreparation.runId()),
        is(3L));
    assertThat(
        scalarLong(
            """
            select count(*)
            from discogs_import_run_chunk
            where import_run_id = ?
              and entity_type = 'label'
            """,
            firstPreparation.runId()),
        is(1L));
    importExecutionCoordinator.complete(false, new IllegalStateException("interrupted"));

    ImportExecutionCoordinator.Preparation resumedPreparation =
        importExecutionCoordinator.prepare(parameters);
    assertThat(resumedPreparation.resumedFromRunId(), is(firstPreparation.runId()));
    assertThat(
        scalarLong(
            "select count(*) from discogs_import_run_chunk where import_run_id = ?",
            resumedPreparation.runId()),
        is(4L));

    createCompletedChunkRewriteGuard();
    try {
      JobExecution resumedExecution =
          jobOperator.start(
              jobOperatorTestUtils.getJob(),
              withPreparation(parameters, resumedPreparation));
      assertThat(resumedExecution.getStatus(), is(BatchStatus.COMPLETED));
      importExecutionCoordinator.complete(true, null);
    } finally {
      dropCompletedChunkRewriteGuard();
      importExecutionCoordinator.complete(false, new IllegalStateException("test cleanup"));
    }

    assertThat(
        scalarLong(
            """
            select count(*)
            from discogs_import_run
            where id in (?, ?)
              and status = 'failed'
            """,
            firstPreparation.runId(),
            resumedPreparation.runId()),
        is(1L));
    assertThat(
        scalarLong(
            "select count(*) from discogs_import_run_chunk where import_run_id = ?",
            resumedPreparation.runId()),
        is(0L));
  }

  final void runAtomicReleaseRetryScenario()
      throws Exception {
    ImportExecutionCoordinator importExecutionCoordinator =
        new ImportExecutionCoordinator(dataSource);
    JobParameters parameters =
        coordinatedParameters(
            1,
            EntityType.ARTIST,
            EntityType.LABEL,
            EntityType.MASTER,
            EntityType.RELEASE);
    executeSql("update master set main_release_id = null");
    executeSql(
        """
        insert into release_item (id, created_at, last_modified_at, title)
        values (1, now(), now(), 'rollback sentinel')
        on conflict (id) do update set title = excluded.title
        """);
    executeSql("delete from release_item_video where release_item_id = 1");
    ImportExecutionCoordinator.Preparation firstPreparation =
        importExecutionCoordinator.prepare(parameters);

    createMasterMainReleaseFailureTrigger();
    JobExecution firstExecution;
    try {
      firstExecution =
          jobOperator.start(
              jobOperatorTestUtils.getJob(),
              withPreparation(parameters, firstPreparation));
    } finally {
      dropMasterMainReleaseFailureTrigger();
    }

    assertThat(firstExecution.getStatus(), is(BatchStatus.FAILED));
    assertThat(
        scalarLong(
            """
            select count(*)
            from discogs_import_run_dump
            where import_run_id = ?
              and entity_type = 'release'
              and completed_at is not null
              and processed_items = total_items
            """,
            firstPreparation.runId()),
        is(0L));
    assertThat(
        scalarLong("select count(*) from release_item where id = 1 and title = 'rollback sentinel'"),
        is(1L));
    assertThat(
        scalarLong("select count(*) from release_item_video where release_item_id = 1"),
        is(0L));
    assertThat(
        scalarLong("select count(*) from master where id = 1 and main_release_id is null"),
        is(1L));
    importExecutionCoordinator.complete(false, new IllegalStateException("interrupted"));

    ImportExecutionCoordinator.Preparation resumedPreparation =
        importExecutionCoordinator.prepare(parameters);
    assertThat(resumedPreparation.resumedFromRunId(), is(firstPreparation.runId()));

    try {
      JobExecution resumedExecution =
          jobOperator.start(
              jobOperatorTestUtils.getJob(),
              withPreparation(parameters, resumedPreparation));
      assertThat(resumedExecution.getStatus(), is(BatchStatus.COMPLETED));
      importExecutionCoordinator.complete(true, null);
    } finally {
      importExecutionCoordinator.complete(false, new IllegalStateException("test cleanup"));
    }

    assertThat(
        scalarLong("select count(*) from release_item where id = 1 and title = 'rollback sentinel'"),
        is(0L));
    assertThat(
        scalarLong("select count(*) from release_item_video where release_item_id = 1"),
        is(6L));
    assertThat(
        scalarLong("select count(*) from master where id = 1 and main_release_id = 1"),
        is(1L));
  }

  final void runIdempotentRefreshScenario() throws Exception {
    JobParameters firstParameters =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(defaultJobParameters())
            .toJobParameters();
    firstParameters = withTrackedImportRun(firstParameters);
    JobExecution first =
        jobOperator.start(jobOperatorTestUtils.getJob(), firstParameters);
    assertThat(first.getExitStatus().getExitCode(), is("COMPLETED"));
    Map<String, String> firstState = businessTableState();

    JobParameters secondParameters =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(defaultJobParameters())
            .toJobParameters();
    secondParameters = withTrackedImportRun(secondParameters);
    JobExecution second =
        jobOperator.start(jobOperatorTestUtils.getJob(), secondParameters);
    assertThat(second.getExitStatus().getExitCode(), is("COMPLETED"));

    Map<String, String> secondState = businessTableState();
    List<String> changedTables =
        firstState.keySet().stream()
            .filter(table -> !Objects.equals(firstState.get(table), secondState.get(table)))
            .toList();
    if (!changedTables.isEmpty()) {
      Path stateDiff = Path.of("build", "reports", "idempotency-state-diff.txt");
      Files.createDirectories(stateDiff.getParent());
      StringBuilder details = new StringBuilder();
      for (String table : changedTables) {
        details
            .append(table)
            .append(" first:\n")
            .append(firstState.get(table))
            .append("\nsecond:\n")
            .append(secondState.get(table))
            .append('\n');
      }
      Files.writeString(stateDiff, details, StandardCharsets.UTF_8);
    }
    assertThat(changedTables, is(List.of()));
    try (InputStream expected =
        Objects.requireNonNull(
            getClass().getResourceAsStream("/test/cross-language-state.json"))) {
      String expectedJson = new String(expected.readAllBytes(), StandardCharsets.UTF_8);
      String actualJson = normalizedBusinessState();
      try (Connection connection = dataSource.getConnection();
          PreparedStatement comparison =
              connection.prepareStatement(
                  """
                  with actual as (select ?::jsonb as value),
                       expected as (select ?::jsonb as value),
                       keys as (
                         select jsonb_object_keys(actual.value) as key from actual
                         union
                         select jsonb_object_keys(expected.value) as key from expected
                       )
                  select coalesce(array_agg(key order by key), '{}'::text[])
                  from keys, actual, expected
                  where exists (
                    (select value from jsonb_array_elements(actual.value -> key)
                     except all
                     select value from jsonb_array_elements(expected.value -> key))
                    union all
                    (select value from jsonb_array_elements(expected.value -> key)
                     except all
                     select value from jsonb_array_elements(actual.value -> key))
                  )
                  """)) {
        comparison.setString(1, actualJson);
        comparison.setString(2, expectedJson);
        try (ResultSet result = comparison.executeQuery()) {
          result.next();
          List<String> mismatchedTables =
              List.of((String[]) result.getArray(1).getArray());
          if (!mismatchedTables.isEmpty()) {
            Path actualOutput = Path.of("build", "reports", "cross-language-actual.json");
            Files.createDirectories(actualOutput.getParent());
            Files.writeString(actualOutput, actualJson, StandardCharsets.UTF_8);
          }
          assertThat(
              mismatchedTables,
              is(List.of()));
        }
      }
    }
  }

  private Map<String, String> businessTableState() throws Exception {
    Map<String, String> state = new LinkedHashMap<>();
    try (Connection connection = dataSource.getConnection();
        Statement tables = connection.createStatement();
        ResultSet tableNames =
            tables.executeQuery(
                """
                select tablename
                from pg_tables
                where schemaname = current_schema()
                  and tablename not like 'batch_%'
                  and tablename not like 'databasechangelog%'
                  and tablename not like 'discogs_%'
                  and tablename <> 'open_discogs_schema_migration'
                order by tablename
                """)) {
      while (tableNames.next()) {
        String tableName = tableNames.getString(1);
        String quoted = "\"" + tableName.replace("\"", "\"\"") + "\"";
        try (Statement rows = connection.createStatement();
            ResultSet serialized =
                rows.executeQuery(
                    "select coalesce(string_agg(row_to_json(row_value)::text, "
                        + "E'\\n' order by row_to_json(row_value)::text), '') "
                        + "from "
                        + quoted
                        + " row_value")) {
          serialized.next();
          state.put(tableName, serialized.getString(1));
        }
      }
    }
    return state;
  }

  private JobParameters withTrackedImportRun(JobParameters parameters) throws Exception {
    long runId;
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement insertRun =
          connection.prepareStatement(
              """
              insert into discogs_import_run
                  (manifest_sha256, status, force_requested, allow_downgrade_requested,
                   processor, processor_version, resumed_from_run_id)
              values (?, 'running', false, false, 'open-discogs-batch', 'test', ?)
              """,
              Statement.RETURN_GENERATED_KEYS)) {
        insertRun.setString(1, "%064x".formatted(TEST_MANIFEST_SEQUENCE.incrementAndGet()));
        insertRun.setNull(2, Types.BIGINT);
        insertRun.executeUpdate();
        try (ResultSet keys = insertRun.getGeneratedKeys()) {
          keys.next();
          runId = keys.getLong(1);
        }
      }

      int chunkSize = Integer.parseInt(parameters.getString(ImportJobParameters.CHUNK_SIZE));
      for (EntityType entityType : EntityType.values()) {
        if (parameters.getParameter(entityType.toString()) == null) {
          continue;
        }
        long dumpId;
        try (PreparedStatement insertDump =
            connection.prepareStatement(
                """
                insert into discogs_dump
                    (etag, dump_date, entity_type, checksum_sha256, size_bytes, uri)
                values (?, current_date, ?, ?, 1, ?)
                on conflict (dump_date, entity_type, checksum_sha256)
                do update set etag = excluded.etag
                returning id
                """)) {
          insertDump.setString(1, "test-" + entityType);
          insertDump.setString(2, entityType.toString());
          insertDump.setString(3, String.valueOf(entityType.ordinal()).repeat(64));
          insertDump.setString(4, "test/" + entityType + ".xml.gz");
          try (ResultSet result = insertDump.executeQuery()) {
            result.next();
            dumpId = result.getLong(1);
          }
        }
        try (PreparedStatement insertRunDump =
            connection.prepareStatement(
                """
                insert into discogs_import_run_dump
                    (import_run_id, entity_type, dump_id, chunk_size,
                     import_contract_revision)
                values (?, ?, ?, ?, ?)
                """)) {
          insertRunDump.setLong(1, runId);
          insertRunDump.setString(2, entityType.toString());
          insertRunDump.setLong(3, dumpId);
          insertRunDump.setInt(4, chunkSize);
          insertRunDump.setInt(
              5, ImportExecution.importContractRevision(entityType.toString()));
          insertRunDump.executeUpdate();
        }
      }
      connection.commit();
    }
    return new JobParametersBuilder(parameters)
        .addLong(ImportJobParameters.RUN_ID, runId)
        .addString(ImportJobParameters.RESUMED, "false")
        .toJobParameters();
  }

  private JobParameters coordinatedParameters(int chunkSize, EntityType... entityTypes) {
    long sequence = TEST_MANIFEST_SEQUENCE.incrementAndGet();
    LocalDate dumpDate = LocalDate.now();
    List<ImportManifest.Dump> manifestDumps = new ArrayList<>(entityTypes.length);
    JobParametersBuilder selected =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addString(ImportJobParameters.CHUNK_SIZE, Integer.toString(chunkSize))
            .addString(ImportJobParameters.FORCE, Boolean.FALSE.toString())
            .addString(ImportJobParameters.ALLOW_DOWNGRADE, Boolean.FALSE.toString());
    for (EntityType entityType : entityTypes) {
      String checksum = "%064x".formatted(sequence + entityType.ordinal());
      manifestDumps.add(
          new ImportManifest.Dump(entityType.toString(), dumpDate, checksum));
      selected
          .addString(entityType.toString(), entityType.toString())
          .addString(ImportJobParameters.checksum(entityType), checksum)
          .addString(ImportJobParameters.date(entityType), dumpDate.toString())
          .addString(ImportJobParameters.etag(entityType), entityType.toString())
          .addString(ImportJobParameters.size(entityType), "1024")
          .addString(
              ImportJobParameters.uri(entityType),
              "test/" + entityType + ".xml.gz");
    }
    return selected
        .addString(
            ImportJobParameters.MANIFEST_SHA256,
            ImportManifest.fingerprint(manifestDumps))
        .toJobParameters();
  }

  private JobParameters withPreparation(
      JobParameters parameters, ImportExecutionCoordinator.Preparation preparation) {
    return new JobParametersBuilder(parameters)
        .addLong(ImportJobParameters.RUN_ID, preparation.runId())
        .addString(
            ImportJobParameters.RESUMED,
            Boolean.toString(preparation.resumedFromRunId() != null))
        .toJobParameters();
  }

  private void createProgressFailureTrigger() throws Exception {
    executeSql(
        """
        create or replace function reject_second_label_chunk()
        returns trigger language plpgsql as $$
        begin
          if new.entity_type = 'label' and new.chunk_index = 1 then
            raise exception 'rejected second label chunk';
          end if;
          return new;
        end
        $$
        """,
        """
        create trigger reject_second_label_chunk
        before insert on discogs_import_run_chunk
        for each row execute function reject_second_label_chunk()
        """);
  }

  private void dropProgressFailureTrigger() throws Exception {
    executeSql(
        "drop trigger if exists reject_second_label_chunk on discogs_import_run_chunk",
        "drop function if exists reject_second_label_chunk()");
  }

  private void createCompletedChunkRewriteGuard() throws Exception {
    executeSql(
        """
        create or replace function reject_completed_relation_rewrite()
        returns trigger language plpgsql as $$
        begin
          raise exception 'completed relation chunk was rewritten';
        end
        $$
        """,
        """
        create trigger reject_completed_artist_url_rewrite
        before insert or update on artist_url
        for each row when (new.artist_id = 1)
        execute function reject_completed_relation_rewrite()
        """,
        """
        create trigger reject_completed_label_url_rewrite
        before insert or update on label_url
        for each row when (new.label_id = 1)
        execute function reject_completed_relation_rewrite()
        """,
        """
        create trigger reject_completed_release_video_rewrite
        before insert or update on release_item_video
        for each row when (new.release_item_id = 1)
        execute function reject_completed_relation_rewrite()
        """);
  }

  private void dropCompletedChunkRewriteGuard() throws Exception {
    executeSql(
        "drop trigger if exists reject_completed_artist_url_rewrite on artist_url",
        "drop trigger if exists reject_completed_label_url_rewrite on label_url",
        "drop trigger if exists reject_completed_release_video_rewrite on release_item_video",
        "drop function if exists reject_completed_relation_rewrite()");
  }

  private void createMasterMainReleaseFailureTrigger() throws Exception {
    executeSql(
        """
        create or replace function reject_master_main_release_update()
        returns trigger language plpgsql as $$
        begin
          raise exception 'rejected master main release update';
        end
        $$
        """,
        """
        create trigger reject_master_main_release_update
        before update of main_release_id on master
        for each row
        when (new.main_release_id is distinct from old.main_release_id)
        execute function reject_master_main_release_update()
        """);
  }

  private void dropMasterMainReleaseFailureTrigger() throws Exception {
    executeSql(
        "drop trigger if exists reject_master_main_release_update on master",
        "drop function if exists reject_master_main_release_update()");
  }

  private void executeSql(String... statements) throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      for (String sql : statements) {
        statement.execute(sql);
      }
    }
  }

  private long scalarLong(String sql, Object... parameters) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < parameters.length; index++) {
        statement.setObject(index + 1, parameters[index]);
      }
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getLong(1);
      }
    }
  }

  private String normalizedBusinessState() throws Exception {
    StringBuilder state = new StringBuilder("{");
    boolean firstTable = true;
    try (Connection connection = dataSource.getConnection();
        Statement tables = connection.createStatement();
        ResultSet tableNames =
            tables.executeQuery(
                """
                select tablename
                from pg_tables
                where schemaname = current_schema()
                  and tablename not like 'batch_%'
                  and tablename not like 'databasechangelog%'
                  and tablename not like 'discogs_%'
                  and tablename <> 'open_discogs_schema_migration'
                order by tablename
                """)) {
      while (tableNames.next()) {
        String tableName = tableNames.getString(1);
        String quoted = "\"" + tableName.replace("\"", "\"\"") + "\"";
        boolean core =
            List.of("artist", "label", "master", "release_item").contains(tableName);
        boolean named = List.of("genre", "style").contains(tableName);
        String projection =
            "to_jsonb(row_value) - 'created_at' - 'last_modified_at'";
        if (!core && !named) {
          projection += " - 'id'";
        }
        try (Statement rows = connection.createStatement();
            ResultSet serialized =
                rows.executeQuery(
                    "select coalesce(jsonb_agg(projected order by projected::text), "
                        + "'[]'::jsonb)::text "
                        + "from (select "
                        + projection
                        + " as projected from "
                        + quoted
                        + " row_value) normalized")) {
          serialized.next();
          if (!firstTable) {
            state.append(',');
          }
          state
              .append('"')
              .append(tableName)
              .append("\":")
              .append(serialized.getString(1));
          firstTable = false;
        }
      }
    }
    return state.append('}').toString();
  }
}
