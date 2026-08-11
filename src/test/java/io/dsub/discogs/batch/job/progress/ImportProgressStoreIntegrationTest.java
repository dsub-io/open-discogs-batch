package io.dsub.discogs.batch.job.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import java.sql.Connection;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class ImportProgressStoreIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final int CHUNK_SIZE = 5;
  private static final String PROCESSOR = "open-discogs-batch";
  private static final String VERSION = "test";

  private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
  private final ImportProgressStore progressStore = new ImportProgressStore(dataSource);
  private final TransactionTemplate transaction =
      new TransactionTemplate(new DataSourceTransactionManager(dataSource));

  @BeforeAll
  static void migrateDatabase() throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet tables =
            connection
                .getMetaData()
                .getTables(null, "public", "discogs_import_run_chunk", new String[] {"TABLE"})) {
      if (tables.next()) {
        return;
      }
    }
    new ResourceDatabasePopulator(
            new ClassPathResource("migrations/V001__initial_schema.sql"),
            new ClassPathResource("migrations/V002__discogs_dump_catalog.sql"),
            new ClassPathResource("migrations/V003__discogs_import_history.sql"),
            new ClassPathResource("migrations/V004__allow_reissued_dump_paths.sql"),
            new ClassPathResource("migrations/V005__durable_import_progress.sql"),
            new ClassPathResource("migrations/V006__concurrent_import_progress.sql"))
        .execute(dataSource);
  }

  @BeforeEach
  void clearState() {
    jdbcTemplate.execute(
        """
        truncate table
            discogs_import_run_chunk,
            discogs_import_run_dump,
            discogs_import_run,
            discogs_dump,
            artist
        restart identity cascade
        """);
  }

  @Test
  void recordsExactChunksAndCompletesEntity() throws Exception {
    long runId = createRun(EntityType.ARTIST);
    ChunkRange first = new ChunkRange(0, 0, CHUNK_SIZE);
    ChunkRange second = new ChunkRange(1, CHUNK_SIZE, 2);

    progressStore.recordCompletedChunk(runId, EntityType.ARTIST, CHUNK_SIZE, first);
    progressStore.recordCompletedChunk(runId, EntityType.ARTIST, CHUNK_SIZE, second);

    ImportProgressSnapshot running = progressStore.getProgress(runId, EntityType.ARTIST);
    assertThat(running.committedItems()).isEqualTo(7);
    assertThat(running.totalItems()).isEmpty();
    assertThat(running.lastCommittedProgressAt()).isPresent();
    assertThat(progressStore.isChunkCompleted(runId, EntityType.ARTIST, first)).isTrue();
    progressStore.completeEntityFromProgress(runId, EntityType.ARTIST, CHUNK_SIZE);
    ImportProgressSnapshot completed = progressStore.getProgress(runId, EntityType.ARTIST);
    assertThat(completed.totalItems()).hasValue(7);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select processed_items = 7
                   and total_items = 7
                   and total_chunks = 2
                   and completed_at is not null
                from discogs_import_run_dump
                where import_run_id = ?
                """,
                Boolean.class,
                runId))
        .isTrue();
  }

  @Test
  void rejectsMismatchedOrDuplicateChunkRanges() throws Exception {
    long runId = createRun(EntityType.LABEL);
    ChunkRange recorded = new ChunkRange(0, 0, CHUNK_SIZE);
    progressStore.recordCompletedChunk(runId, EntityType.LABEL, CHUNK_SIZE, recorded);

    assertThatThrownBy(
            () ->
                progressStore.isChunkCompleted(
                    runId,
                    EntityType.LABEL,
                    new ChunkRange(0, 1, CHUNK_SIZE)))
        .hasMessageContaining("does not match the source range");
    assertThatThrownBy(
            () ->
                progressStore.isChunkCompleted(
                    runId,
                    EntityType.LABEL,
                    new ChunkRange(0, 0, CHUNK_SIZE - 1)))
        .hasMessageContaining("does not match the source range");
    assertThatThrownBy(
            () ->
                progressStore.recordCompletedChunk(
                    runId, EntityType.LABEL, CHUNK_SIZE, recorded))
        .hasMessageContaining("progress already exists");
  }

  @Test
  void missingChunkIsNotCompleted() throws Exception {
    long runId = createRun(EntityType.ARTIST);

    assertThat(
            progressStore.isChunkCompleted(
                runId, EntityType.ARTIST, new ChunkRange(0, 0, CHUNK_SIZE)))
        .isFalse();
  }

  @Test
  void emptyEntityCompletesWithZeroChunks() throws Exception {
    long runId = createRun(EntityType.LABEL);

    progressStore.completeEntity(runId, EntityType.LABEL, CHUNK_SIZE, 0);

    assertThat(
            jdbcTemplate.queryForObject(
                """
                select total_items = 0
                   and total_chunks = 0
                   and processed_items = 0
                   and completed_at is not null
                from discogs_import_run_dump
                where import_run_id = ?
                """,
                Boolean.class,
                runId))
        .isTrue();
  }

  @Test
  void activeRunFenceAcceptsItsOwningRun() throws Exception {
    long runId = createRun(EntityType.MASTER);

    progressStore.fenceActiveRun(runId);
  }

  @Test
  void completionRejectsAChunkIndexOutsideTheSourceCoverage() throws Exception {
    long runId = createRun(EntityType.MASTER);
    progressStore.recordCompletedChunk(
        runId, EntityType.MASTER, CHUNK_SIZE, new ChunkRange(0, 0, CHUNK_SIZE));
    progressStore.recordCompletedChunk(
        runId, EntityType.MASTER, CHUNK_SIZE, new ChunkRange(1, 5, CHUNK_SIZE));
    progressStore.recordCompletedChunk(
        runId, EntityType.MASTER, CHUNK_SIZE, new ChunkRange(3, 15, CHUNK_SIZE));

    assertThatThrownBy(
            () ->
                progressStore.completeEntityFromProgress(
                    runId, EntityType.MASTER, CHUNK_SIZE))
        .hasMessageContaining("chunk coverage does not match");
  }

  @Test
  void inactiveRunFenceRollsBackLateCanonicalWrites() {
    long runId = createRun(EntityType.ARTIST);
    jdbcTemplate.update(
        "update discogs_import_run set status = 'failed', completed_at = now() where id = ?",
        runId);

    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status -> {
                      jdbcTemplate.update(
                          """
                          insert into artist (id, created_at, last_modified_at, name)
                          values (9001, now(), now(), 'late')
                          """);
                      try {
                        progressStore.fenceActiveRun(runId);
                      } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                      }
                    }))
        .hasRootCauseMessage("import run is not active: " + runId);
    assertThat(jdbcTemplate.queryForObject("select count(*) from artist", Long.class)).isZero();
  }

  @Test
  void ledgerFailureRollsBackCanonicalChunkAndSummary() {
    long runId = createRun(EntityType.RELEASE);
    jdbcTemplate.execute(
        """
        create or replace function reject_test_chunk() returns trigger language plpgsql as $$
        begin
          if new.chunk_index = 1 then
            raise exception 'rejected test chunk';
          end if;
          return new;
        end
        $$
        """);
    jdbcTemplate.execute(
        """
        create trigger reject_test_chunk
        before insert on discogs_import_run_chunk
        for each row execute function reject_test_chunk()
        """);

    try {
      assertThatThrownBy(
              () ->
                  transaction.executeWithoutResult(
                      status -> {
                        jdbcTemplate.update(
                            """
                            insert into artist (id, created_at, last_modified_at, name)
                            values (9002, now(), now(), 'rolled back')
                            """);
                        try {
                          progressStore.recordCompletedChunk(
                              runId,
                              EntityType.RELEASE,
                              CHUNK_SIZE,
                              new ChunkRange(1, CHUNK_SIZE, CHUNK_SIZE));
                        } catch (Exception exception) {
                          throw new IllegalStateException(exception);
                        }
                      }))
          .rootCause()
          .hasMessageContaining("rejected test chunk");
    } finally {
      jdbcTemplate.execute("drop trigger if exists reject_test_chunk on discogs_import_run_chunk");
      jdbcTemplate.execute("drop function if exists reject_test_chunk()");
    }

    assertThat(jdbcTemplate.queryForObject("select count(*) from artist", Long.class)).isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select processed_items from discogs_import_run_dump where import_run_id = ?",
                Long.class,
                runId))
        .isZero();
  }

  private long createRun(EntityType entityType) {
    Long dumpId =
        jdbcTemplate.queryForObject(
            """
            insert into discogs_dump
                (etag, dump_date, entity_type, checksum_sha256, size_bytes, uri)
            values ('test', current_date, ?, ?, 1, 'test.xml.gz')
            returning id
            """,
            Long.class,
            entityType.toString(),
            String.valueOf(entityType.ordinal()).repeat(64));
    Long runId =
        jdbcTemplate.queryForObject(
            """
            insert into discogs_import_run
                (manifest_sha256, status, force_requested, allow_downgrade_requested,
                 processor, processor_version)
            values (?, 'running', false, false, ?, ?)
            returning id
            """,
            Long.class,
            "a".repeat(64),
            PROCESSOR,
            VERSION);
    jdbcTemplate.update(
        """
        insert into discogs_import_run_dump
            (import_run_id, entity_type, dump_id, chunk_size)
        values (?, ?, ?, ?)
        """,
        runId,
        entityType.toString(),
        dumpId,
        CHUNK_SIZE);
    return runId;
  }
}
