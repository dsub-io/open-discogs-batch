package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;

class DefaultJooqMasterMainReleaseItemWriterIntegrationTest
    extends PostgreSQLIntegrationSupport {

  private static final int RELEASE_A = 10;
  private static final int RELEASE_B = 20;
  private static final int RELEASE_C = 30;
  private static final int MASTER_A = 1;
  private static final int MASTER_B = 2;
  private static final int MASTER_C = 3;

  private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
  private final DefaultJooqMasterMainReleaseItemWriter writer = writer();

  @BeforeAll
  static void migrateDatabase() throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet tables =
            connection
                .getMetaData()
                .getTables(null, "public", "master", new String[] {"TABLE"})) {
      if (tables.next()) {
        return;
      }
    }
    new ResourceDatabasePopulator(new ClassPathResource("migrations/V001__initial_schema.sql"))
        .execute(dataSource);
  }

  @BeforeEach
  void resetMappings() {
    jdbcTemplate.execute("truncate table master, release_item restart identity cascade");
    jdbcTemplate.update(
        """
        insert into master (id, created_at, last_modified_at, title, main_release_id)
        values
          (?, now(), timestamp '2026-01-01 00:00:00', 'master-a', null),
          (?, now(), timestamp '2026-01-01 00:00:00', 'master-b', null),
          (?, now(), timestamp '2026-01-01 00:00:00', 'master-c', null)
        """,
        MASTER_A,
        MASTER_B,
        MASTER_C);
    jdbcTemplate.update(
        """
        insert into release_item (id, created_at, last_modified_at, title, master_id)
        values
          (?, now(), now(), 'release-a', ?),
          (?, now(), now(), 'release-b', ?),
          (?, now(), now(), 'release-c', null)
        """,
        RELEASE_A,
        MASTER_A,
        RELEASE_B,
        MASTER_C,
        RELEASE_C);
    jdbcTemplate.update(
        """
        update master
        set main_release_id = case id when ? then ? when ? then ? end
        where id in (?, ?)
        """,
        MASTER_A,
        RELEASE_A,
        MASTER_C,
        RELEASE_B,
        MASTER_A,
        MASTER_C);
  }

  @Test
  void movesARootFromOneMasterToAnother() {
    setReleaseMaster(RELEASE_A, MASTER_B);
    writer.write(chunk(assignment(RELEASE_A, MASTER_B)));

    assertThat(mainReleaseId(MASTER_A)).isNull();
    assertThat(mainReleaseId(MASTER_B)).isEqualTo(RELEASE_A);
  }

  @Test
  void clearsMappingsForFalseOrRemovedRelations() {
    setReleaseMaster(RELEASE_A, null);
    setReleaseMaster(RELEASE_B, null);
    writer.write(
        chunk(assignment(RELEASE_A, null), assignment(RELEASE_B, null)));

    assertThat(mainReleaseId(MASTER_A)).isNull();
    assertThat(mainReleaseId(MASTER_C)).isNull();
  }

  @Test
  void leavesAnUnchangedMappingUntouchedAcrossRetries() {
    LocalDateTime before = lastModifiedAt(MASTER_A);
    MasterMainReleaseAssignment unchanged = assignment(RELEASE_A, MASTER_A);

    writer.write(chunk(unchanged));
    writer.write(chunk(unchanged));

    assertThat(mainReleaseId(MASTER_A)).isEqualTo(RELEASE_A);
    assertThat(lastModifiedAt(MASTER_A)).isEqualTo(before);
  }

  @Test
  void rollsBackAnInterruptedChunkAndConvergesOnRetry() {
    TransactionTemplate transaction =
        new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    MasterMainReleaseAssignment moved = assignment(RELEASE_A, MASTER_B);
    setReleaseMaster(RELEASE_A, MASTER_B);

    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    ignored -> {
                      writer.write(chunk(moved));
                      throw new SimulatedInterruption();
                    }))
        .isInstanceOf(SimulatedInterruption.class);
    assertThat(mainReleaseId(MASTER_A)).isNull();
    assertThat(mainReleaseId(MASTER_B)).isNull();

    transaction.executeWithoutResult(ignored -> writer.write(chunk(moved)));

    assertThat(mainReleaseId(MASTER_A)).isNull();
    assertThat(mainReleaseId(MASTER_B)).isEqualTo(RELEASE_A);
  }

  private DefaultJooqMasterMainReleaseItemWriter writer() {
    DSLContext context = DSL.using(dataSource, SQLDialect.POSTGRES);
    return new DefaultJooqMasterMainReleaseItemWriter(context);
  }

  private Chunk<MasterMainReleaseAssignment> chunk(
      MasterMainReleaseAssignment... assignments) {
    return new Chunk<>(List.of(assignments));
  }

  private MasterMainReleaseAssignment assignment(int releaseId, Integer masterId) {
    return new MasterMainReleaseAssignment(
        releaseId, masterId, LocalDateTime.of(2026, 8, 12, 12, 0));
  }

  private Integer mainReleaseId(int masterId) {
    return jdbcTemplate.queryForObject(
        "select main_release_id from master where id = ?", Integer.class, masterId);
  }

  private void setReleaseMaster(int releaseId, Integer masterId) {
    jdbcTemplate.update(
        "update release_item set master_id = ? where id = ?", masterId, releaseId);
  }

  private LocalDateTime lastModifiedAt(int masterId) {
    return jdbcTemplate.queryForObject(
        "select last_modified_at from master where id = ?", LocalDateTime.class, masterId);
  }

  private static final class SimulatedInterruption extends RuntimeException {}
}
