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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.batch.core.ExitStatus;
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

  @Test
  void whenAllTypesProvided__ShouldNotSkipAnyType() throws Exception {
    JobParameters params =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(defaultJobParameters())
            .toJobParameters();

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

  @Test
  void whenOnlyArtistLabel__ShouldSkipMasterRelease() throws Exception {
    JobParametersBuilder builder = new JobParametersBuilder();
    //    builder.addString("artist", "artist");
    builder.addString("label", "label");
    builder.addString("chunkSize", "1000");
    JobParameters params =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(builder.toJobParameters())
            .toJobParameters();

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

  @Test
  void whenSameDumpIsForcedTwice__BusinessRowsRemainIdentical() throws Exception {
    JobParameters firstParameters =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(defaultJobParameters())
            .toJobParameters();
    JobExecution first =
        jobOperator.start(jobOperatorTestUtils.getJob(), firstParameters);
    assertThat(first.getExitStatus().getExitCode(), is("COMPLETED"));
    Map<String, String> firstState = businessTableState();

    JobParameters secondParameters =
        jobOperatorTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(defaultJobParameters())
            .toJobParameters();
    JobExecution second =
        jobOperator.start(jobOperatorTestUtils.getJob(), secondParameters);
    assertThat(second.getExitStatus().getExitCode(), is("COMPLETED"));

    Map<String, String> secondState = businessTableState();
    List<String> changedTables =
        firstState.keySet().stream()
            .filter(table -> !Objects.equals(firstState.get(table), secondState.get(table)))
            .toList();
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
                where schemaname = 'public'
                  and tablename not like 'batch_%'
                  and tablename not like 'databasechangelog%'
                  and tablename not like 'discogs_%'
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
                where schemaname = 'public'
                  and tablename not like 'batch_%'
                  and tablename not like 'databasechangelog%'
                  and tablename not like 'discogs_%'
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
