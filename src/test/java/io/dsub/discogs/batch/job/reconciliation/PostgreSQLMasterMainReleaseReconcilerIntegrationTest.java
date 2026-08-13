package io.dsub.discogs.batch.job.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class PostgreSQLMasterMainReleaseReconcilerIntegrationTest
    extends PostgreSQLIntegrationSupport {

  private static final int MASTER_A = 1;
  private static final int MASTER_B = 2;
  private static final int MASTER_C = 3;
  private static final int RELEASE_A = 10;
  private static final int RELEASE_B = 20;
  private static final int RELEASE_C = 30;
  private static final LocalDateTime SECOND_OBSERVATION =
      LocalDateTime.of(2026, 8, 2, 0, 0);
  private static final LocalDateTime THIRD_OBSERVATION =
      LocalDateTime.of(2026, 8, 3, 0, 0);
  private static final LocalDateTime FOURTH_OBSERVATION =
      LocalDateTime.of(2026, 8, 4, 0, 0);

  private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
  private final PostgreSQLMasterMainReleaseReconciler reconciler =
      new PostgreSQLMasterMainReleaseReconciler(dataSource);
  private final TransactionTemplate transaction =
      new TransactionTemplate(new DataSourceTransactionManager(dataSource));

  @BeforeEach
  void resetMappings() {
    jdbcTemplate.execute("truncate table master, release_item restart identity cascade");
    jdbcTemplate.update(
        """
        insert into master (id, created_at, last_modified_at, title)
        values
          (?, now(), timestamp '2026-01-01 00:00:00', 'master-a'),
          (?, now(), timestamp '2026-01-01 00:00:00', 'master-b'),
          (?, now(), timestamp '2026-01-01 00:00:00', 'master-c')
        """,
        MASTER_A,
        MASTER_B,
        MASTER_C);
    jdbcTemplate.update(
        """
        insert into release_item
            (id, created_at, last_modified_at, title, master_id, is_master)
        values
          (?, now(), timestamp '2026-08-01 00:00:00', 'release-a', ?, true),
          (?, now(), timestamp '2026-08-01 00:00:00', 'release-b', ?, true),
          (?, now(), timestamp '2026-08-01 00:00:00', 'release-c', null, false)
        """,
        RELEASE_A,
        MASTER_A,
        RELEASE_B,
        MASTER_C,
        RELEASE_C);
  }

  @Test
  void reconcilesAssignmentsMovesClearsAndLatestCanonicalRoot() {
    transaction.executeWithoutResult(ignored -> reconciler.reconcile());
    assertThat(mainReleaseId(MASTER_A)).isEqualTo(RELEASE_A);
    assertThat(mainReleaseId(MASTER_C)).isEqualTo(RELEASE_B);

    updateRelease(RELEASE_A, MASTER_B, true, SECOND_OBSERVATION);
    transaction.executeWithoutResult(ignored -> reconciler.reconcile());
    assertThat(mainReleaseId(MASTER_A)).isNull();
    assertThat(mainReleaseId(MASTER_B)).isEqualTo(RELEASE_A);

    updateRelease(RELEASE_A, MASTER_B, false, THIRD_OBSERVATION);
    transaction.executeWithoutResult(ignored -> reconciler.reconcile());
    assertThat(mainReleaseId(MASTER_B)).isNull();

    updateRelease(RELEASE_C, MASTER_C, true, FOURTH_OBSERVATION);
    transaction.executeWithoutResult(ignored -> reconciler.reconcile());
    assertThat(mainReleaseId(MASTER_C)).isEqualTo(RELEASE_C);
  }

  @Test
  void leavesAnUnchangedMappingUntouched() {
    transaction.executeWithoutResult(ignored -> reconciler.reconcile());
    LocalDateTime before = lastModifiedAt(MASTER_A);

    transaction.executeWithoutResult(ignored -> reconciler.reconcile());

    assertThat(mainReleaseId(MASTER_A)).isEqualTo(RELEASE_A);
    assertThat(lastModifiedAt(MASTER_A)).isEqualTo(before);
  }

  @Test
  void rollsBackClearAndSetTogetherThenConvergesOnRetry() {
    transaction.executeWithoutResult(ignored -> reconciler.reconcile());
    updateRelease(RELEASE_C, MASTER_A, true, FOURTH_OBSERVATION);

    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    ignored -> {
                      reconciler.reconcile();
                      throw new SimulatedInterruption();
                    }))
        .isInstanceOf(SimulatedInterruption.class);
    assertThat(mainReleaseId(MASTER_A)).isEqualTo(RELEASE_A);

    transaction.executeWithoutResult(ignored -> reconciler.reconcile());
    assertThat(mainReleaseId(MASTER_A)).isEqualTo(RELEASE_C);
  }

  private Integer mainReleaseId(int masterId) {
    return jdbcTemplate.queryForObject(
        "select main_release_id from master where id = ?", Integer.class, masterId);
  }

  private LocalDateTime lastModifiedAt(int masterId) {
    return jdbcTemplate.queryForObject(
        "select last_modified_at from master where id = ?", LocalDateTime.class, masterId);
  }

  private void updateRelease(
      int releaseId, Integer masterId, boolean mainRelease, LocalDateTime observedAt) {
    jdbcTemplate.update(
        """
        update release_item
        set master_id = ?, is_master = ?, last_modified_at = ?
        where id = ?
        """,
        masterId,
        mainRelease,
        observedAt,
        releaseId);
  }

  private static final class SimulatedInterruption extends RuntimeException {}
}
