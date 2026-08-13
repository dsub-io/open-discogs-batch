package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.opendiscogs.model.manifest.ImportManifest;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/** Child process that commits one Release chunk and then waits to be forcibly terminated. */
public final class ReleaseProcessKillFixture {

  static final String JDBC_URL_ENV = "OPEN_DISCOGS_RELEASE_KILL_JDBC_URL";
  static final String JDBC_USERNAME_ENV = "OPEN_DISCOGS_RELEASE_KILL_JDBC_USERNAME";
  static final String JDBC_PASSWORD_ENV = "OPEN_DISCOGS_RELEASE_KILL_JDBC_PASSWORD";
  static final String READY_PREFIX = "OPEN_DISCOGS_RELEASE_CHUNK_READY:";
  static final int CHUNK_SIZE = 1;
  static final int RELEASE_ID = 900_001;

  private static final LocalDate DUMP_DATE = LocalDate.of(2026, 7, 1);
  private static final String CHECKSUM = "9".repeat(64);
  private static final String ETAG = "release-process-kill-etag";
  private static final String URI = "data/2026/discogs_20260701_releases.xml.gz";
  private static final String PROCESSOR_TITLE = "process-kill-fixture";

  private ReleaseProcessKillFixture() {
  }

  public static void main(String[] arguments) throws Exception {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(requiredEnvironment(JDBC_URL_ENV));
    dataSource.setUsername(requiredEnvironment(JDBC_USERNAME_ENV));
    dataSource.setPassword(requiredEnvironment(JDBC_PASSWORD_ENV));

    ImportExecutionCoordinator coordinator = new ImportExecutionCoordinator(dataSource);
    ImportExecutionCoordinator.Preparation preparation = coordinator.prepare(jobParameters());
    ImportProgressStore progressStore = new ImportProgressStore(dataSource);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    TransactionTemplate transaction =
        new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    transaction.executeWithoutResult(
        ignored -> {
          jdbcTemplate.update(
              """
              insert into release_item (id, created_at, last_modified_at, title)
              values (?, now(), now(), ?)
              """,
              RELEASE_ID,
              PROCESSOR_TITLE);
          try {
            progressStore.recordCompletedChunk(
                preparation.runId(),
                EntityType.RELEASE,
                CHUNK_SIZE,
                new ChunkRange(0, 0, CHUNK_SIZE));
          } catch (Exception exception) {
            throw new IllegalStateException("failed to commit the Release fixture chunk", exception);
          }
        });

    System.out.println(READY_PREFIX + preparation.runId());
    System.out.flush();
    new CountDownLatch(1).await();
  }

  static JobParameters jobParameters() {
    ImportManifest.Dump dump =
        new ImportManifest.Dump(EntityType.RELEASE.toString(), DUMP_DATE, CHECKSUM);
    return new JobParametersBuilder()
        .addString(ImportJobParameters.CHUNK_SIZE, String.valueOf(CHUNK_SIZE))
        .addString(ImportJobParameters.FORCE, Boolean.FALSE.toString())
        .addString(ImportJobParameters.ALLOW_DOWNGRADE, Boolean.FALSE.toString())
        .addString(EntityType.RELEASE.toString(), ETAG)
        .addString(ImportJobParameters.checksum(EntityType.RELEASE), CHECKSUM)
        .addString(ImportJobParameters.date(EntityType.RELEASE), DUMP_DATE.toString())
        .addString(ImportJobParameters.etag(EntityType.RELEASE), ETAG)
        .addString(ImportJobParameters.size(EntityType.RELEASE), "2")
        .addString(ImportJobParameters.uri(EntityType.RELEASE), URI)
        .addString(
            ImportJobParameters.MANIFEST_SHA256,
            ImportManifest.fingerprint(List.of(dump)))
        .toJobParameters();
  }

  private static String requiredEnvironment(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("missing child-process environment: " + name);
    }
    return value;
  }
}
