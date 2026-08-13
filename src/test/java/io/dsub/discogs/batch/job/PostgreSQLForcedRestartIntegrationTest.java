package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import com.github.dockerjava.api.model.ExposedPort;
import io.dsub.discogs.batch.config.CanonicalSchemaMigrator;
import io.dsub.discogs.batch.config.DatabaseSchema;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
import io.dsub.discogs.batch.job.processor.ReleaseRootMutation;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.writer.DurableReleaseItemWriter;
import io.dsub.opendiscogs.model.manifest.ImportManifest;
import java.time.Duration;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PostgreSQLForcedRestartIntegrationTest {

  private static final String POSTGRES_IMAGE = "postgres:18.4-alpine";
  private static final String POSTGRES_DATA_DIRECTORY = "/var/lib/postgresql";
  private static final String DATABASE_NAME = "databaseName";
  private static final String DATABASE_USERNAME = "username";
  private static final String DATABASE_PASSWORD = "password";
  private static final String RESOURCE_LABEL = "io.dsub.test-resource";
  private static final String RESOURCE_VALUE = "postgres-forced-restart";
  private static final String RUN_LABEL = "io.dsub.test-run";
  private static final String OWNER_LABEL = "io.dsub.test-owner";
  private static final String OWNER_VALUE = "open-discogs-batch";
  private static final String KILL_SIGNAL = "KILL";
  private static final Duration RESTART_TIMEOUT = Duration.ofSeconds(10);
  private static final LocalDate DUMP_DATE = LocalDate.of(2026, 7, 1);
  private static final int CHUNK_SIZE = 1;
  private static final int RELEASE_ID = 900_002;

  @Test
  void releaseResumesAfterPostgresIsKilledAndRestarted() throws Exception {
    try (RestartablePostgres postgres = RestartablePostgres.start()) {
      DataSource initialDataSource = postgres.dataSource();
      new CanonicalSchemaMigrator(
              initialDataSource, new DatabaseSchema(DatabaseSchema.DEFAULT_NAME))
          .migrate();
      seedReleaseDependencyCheckpoints(initialDataSource);

      ImportExecutionCoordinator interrupted =
          new ImportExecutionCoordinator(initialDataSource);
      ImportExecutionCoordinator.Preparation preparation =
          interrupted.prepare(releaseParameters());
      commitReleaseChunk(initialDataSource, preparation.runId());

      postgres.kill();
      assertThatThrownBy(
              () ->
                  interrupted.complete(
                      false, new IllegalStateException("PostgreSQL was killed")))
          .isInstanceOf(ImportExecutionException.class)
          .hasMessageContaining("failed to complete import execution");
      postgres.restart();

      DataSource restartedDataSource = postgres.dataSource();
      JdbcTemplate jdbc = new JdbcTemplate(restartedDataSource);
      assertThat(
              jdbc.queryForObject(
                  "select status from discogs_import_run where id = ?",
                  String.class,
                  preparation.runId()))
          .isEqualTo("running");
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from discogs_import_run_chunk where import_run_id = ?",
                  Long.class,
                  preparation.runId()))
          .isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from release_item where id = ?",
                  Long.class,
                  RELEASE_ID))
          .isEqualTo(1);

      ImportExecutionCoordinator retry =
          new ImportExecutionCoordinator(restartedDataSource);
      ImportExecutionCoordinator.Preparation resumed = retry.prepare(releaseParameters());
      assertThat(resumed.resumedFromRunId()).isEqualTo(preparation.runId());
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from discogs_import_run_chunk where import_run_id = ?",
                  Long.class,
                  preparation.runId()))
          .isZero();

      ImportProgressStore progressStore = new ImportProgressStore(restartedDataSource);
      verifyCommittedChunkIsSkipped(progressStore, resumed.runId());
      progressStore.recordCompletedChunk(
          resumed.runId(),
          EntityType.RELEASE,
          CHUNK_SIZE,
          new ChunkRange(1, 1, CHUNK_SIZE));
      progressStore.completeEntity(
          resumed.runId(), EntityType.RELEASE, CHUNK_SIZE, 2);
      retry.complete(true, null);

      assertThat(
              jdbc.queryForObject(
                  "select status from discogs_import_run where id = ?",
                  String.class,
                  resumed.runId()))
          .isEqualTo("success");
    }
  }

  private void seedReleaseDependencyCheckpoints(DataSource dataSource) throws Exception {
    List<SelectedDump> dependencies =
        List.of(
            new SelectedDump(EntityType.ARTIST, 'a'),
            new SelectedDump(EntityType.LABEL, 'b'),
            new SelectedDump(EntityType.MASTER, 'c'));
    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation preparation =
        coordinator.prepare(parameters(dependencies));
    markEntitiesComplete(dataSource, preparation.runId());
    coordinator.complete(true, null);
  }

  private void commitReleaseChunk(DataSource dataSource, long runId) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    ImportProgressStore progressStore = new ImportProgressStore(dataSource);
    TransactionTemplate transaction =
        new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    transaction.executeWithoutResult(
        ignored -> {
          jdbc.update(
              """
              insert into release_item (id, created_at, last_modified_at, title)
              values (?, now(), now(), 'forced restart fixture')
              """,
              RELEASE_ID);
          try {
            progressStore.recordCompletedChunk(
                runId,
                EntityType.RELEASE,
                CHUNK_SIZE,
                new ChunkRange(0, 0, CHUNK_SIZE));
          } catch (ImportExecutionException exception) {
            throw new IllegalStateException("commit Release progress", exception);
          }
        });
  }

  private void verifyCommittedChunkIsSkipped(
      ImportProgressStore progressStore, long runId) throws Exception {
    DurableReleaseItemWriter writer =
        new DurableReleaseItemWriter(
            ignored -> {
              throw new AssertionError("committed Release root chunk was rewritten");
            },
            ignored -> {
              throw new AssertionError("committed Release relation chunk was rewritten");
            },
            progressStore,
            runId,
            CHUNK_SIZE,
            true);
    writer.write(
        new Chunk<>(
            List.of(
                new ProcessedChunk<>(
                    new ChunkRange(0, 0, CHUNK_SIZE),
                    List.<ReleaseRootMutation>of()))));
  }

  private void markEntitiesComplete(DataSource dataSource, long runId) {
    new JdbcTemplate(dataSource)
        .update(
            """
            update discogs_import_run_dump
            set total_items = processed_items,
                total_chunks = 0,
                last_progress_at = now(),
                completed_at = now()
            where import_run_id = ?
            """,
            runId);
  }

  private JobParameters releaseParameters() {
    return parameters(List.of(new SelectedDump(EntityType.RELEASE, 'd')));
  }

  private JobParameters parameters(List<SelectedDump> selectedDumps) {
    List<ImportManifest.Dump> manifestDumps = new ArrayList<>(selectedDumps.size());
    JobParametersBuilder builder =
        new JobParametersBuilder()
            .addString(ImportJobParameters.CHUNK_SIZE, String.valueOf(CHUNK_SIZE))
            .addString(ImportJobParameters.FORCE, Boolean.FALSE.toString())
            .addString(ImportJobParameters.ALLOW_DOWNGRADE, Boolean.FALSE.toString());
    for (SelectedDump selected : selectedDumps) {
      String checksum = Character.toString(selected.checksum()).repeat(64);
      EntityType entityType = selected.entityType();
      manifestDumps.add(
          new ImportManifest.Dump(entityType.toString(), DUMP_DATE, checksum));
      builder
          .addString(entityType.toString(), entityType + "-restart-etag")
          .addString(ImportJobParameters.checksum(entityType), checksum)
          .addString(ImportJobParameters.date(entityType), DUMP_DATE.toString())
          .addString(ImportJobParameters.etag(entityType), entityType + "-restart-etag")
          .addString(ImportJobParameters.size(entityType), "2")
          .addString(
              ImportJobParameters.uri(entityType),
              "data/2026/discogs_20260701_" + entityType + "s.xml.gz");
    }
    return builder
        .addString(
            ImportJobParameters.MANIFEST_SHA256,
            ImportManifest.fingerprint(manifestDumps))
        .toJobParameters();
  }

  private record SelectedDump(EntityType entityType, char checksum) {
  }

  private static final class RestartablePostgres implements AutoCloseable {

    private final DockerClient docker;
    private final PostgreSQLContainer container;
    private final String volumeName;
    private int mappedPort;

    private RestartablePostgres(
        DockerClient docker, PostgreSQLContainer container, String volumeName) {
      this.docker = docker;
      this.container = container;
      this.volumeName = volumeName;
      this.mappedPort = container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
    }

    static RestartablePostgres start() {
      DockerClient docker = DockerClientFactory.instance().client();
      String runId = "postgres-restart-" + ProcessHandle.current().pid() + "-" + System.nanoTime();
      String volumeName = "open-discogs-" + runId;
      Map<String, String> labels =
          Map.of(
              OWNER_LABEL,
              OWNER_VALUE,
              RUN_LABEL,
              runId,
              RESOURCE_LABEL,
              RESOURCE_VALUE);
      docker.createVolumeCmd().withName(volumeName).withLabels(labels).exec();
      PostgreSQLContainer container =
          new PostgreSQLContainer(POSTGRES_IMAGE)
              .withDatabaseName(DATABASE_NAME)
              .withUsername(DATABASE_USERNAME)
              .withPassword(DATABASE_PASSWORD)
              .withLabels(labels)
              .withCreateContainerCmdModifier(
                  command ->
                      command
                          .getHostConfig()
                          .withMounts(
                              List.of(
                                  new Mount()
                                      .withType(MountType.VOLUME)
                                      .withSource(volumeName)
                                      .withTarget(POSTGRES_DATA_DIRECTORY)
                                      .withReadOnly(false))))
              .waitingFor(Wait.forListeningPort().withStartupTimeout(RESTART_TIMEOUT));
      try {
        container.start();
        var mounts = docker.inspectContainerCmd(container.getContainerId()).exec().getMounts();
        assertThat(mounts).hasSize(1);
        assertThat(mounts.getFirst().getName()).isEqualTo(volumeName);
        assertThat(mounts.getFirst().getDestination().getPath())
            .isEqualTo(POSTGRES_DATA_DIRECTORY);
        assertThat(mounts.getFirst().getRW()).isTrue();
        return new RestartablePostgres(docker, container, volumeName);
      } catch (RuntimeException | AssertionError failure) {
        try {
          container.stop();
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
        try {
          docker.removeVolumeCmd(volumeName).exec();
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
        throw failure;
      }
    }

    DataSource dataSource() {
      DriverManagerDataSource dataSource = new DriverManagerDataSource();
      dataSource.setDriverClassName(container.getDriverClassName());
      dataSource.setUrl(
          "jdbc:postgresql://"
              + container.getHost()
              + ":"
              + mappedPort
              + "/"
              + DATABASE_NAME);
      dataSource.setUsername(container.getUsername());
      dataSource.setPassword(container.getPassword());
      return dataSource;
    }

    void kill() {
      String containerId = container.getContainerId();
      try (WaitContainerResultCallback stopped = docker.waitContainerCmd(containerId).start()) {
        docker.killContainerCmd(containerId).withSignal(KILL_SIGNAL).exec();
        assertThat(stopped.awaitStatusCode(RESTART_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
            .isNotZero();
      } catch (Exception exception) {
        throw new IllegalStateException("kill PostgreSQL test container", exception);
      }
      assertThat(docker.inspectContainerCmd(containerId).exec().getState().getRunning())
          .isFalse();
    }

    void restart() {
      docker.startContainerCmd(container.getContainerId()).exec();
      var bindings =
          docker
              .inspectContainerCmd(container.getContainerId())
              .exec()
              .getNetworkSettings()
              .getPorts()
              .getBindings()
              .get(ExposedPort.tcp(PostgreSQLContainer.POSTGRESQL_PORT));
      assertThat(bindings).isNotEmpty();
      assertThat(List.of(bindings))
          .extracting(binding -> binding.getHostPortSpec())
          .containsOnly(bindings[0].getHostPortSpec());
      mappedPort = Integer.parseInt(bindings[0].getHostPortSpec());
      Unreliables.retryUntilSuccess(
          Math.toIntExact(RESTART_TIMEOUT.toSeconds()),
          TimeUnit.SECONDS,
          () -> {
            try (Connection connection = dataSource().getConnection();
                Statement statement = connection.createStatement()) {
              statement.execute("select 1");
              return true;
            }
          });
    }

    @Override
    public void close() {
      String containerId = container.getContainerId();
      RuntimeException cleanupFailure = null;
      try {
        container.stop();
      } catch (RuntimeException failure) {
        cleanupFailure = failure;
      }
      boolean containerRemoved;
      try {
        docker.inspectContainerCmd(containerId).exec();
        containerRemoved = false;
      } catch (NotFoundException expected) {
        containerRemoved = true;
      }
      try {
        docker.removeVolumeCmd(volumeName).exec();
      } catch (RuntimeException failure) {
        if (cleanupFailure == null) {
          cleanupFailure = failure;
        } else {
          cleanupFailure.addSuppressed(failure);
        }
      }
      boolean volumeRemoved;
      try {
        docker.inspectVolumeCmd(volumeName).exec();
        volumeRemoved = false;
      } catch (NotFoundException expected) {
        volumeRemoved = true;
      }
      if (cleanupFailure != null) {
        throw cleanupFailure;
      }
      assertThat(containerRemoved).isTrue();
      assertThat(volumeRemoved).isTrue();
    }
  }
}
