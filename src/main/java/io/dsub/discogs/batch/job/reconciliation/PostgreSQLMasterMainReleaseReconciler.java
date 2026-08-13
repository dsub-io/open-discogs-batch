package io.dsub.discogs.batch.job.reconciliation;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL set reconciliation for the denormalized Master main-release backlink.
 *
 * <p>Historical roots are retained, so the newest observed main root wins; Release ID is the
 * deterministic tie-breaker for equal timestamps.
 */
@Component
public final class PostgreSQLMasterMainReleaseReconciler
    implements MasterMainReleaseReconciler {

  static final String CLEAR_STALE_MAPPINGS_SQL =
      """
      with desired as materialized (
        select distinct on (release_item.master_id)
               release_item.master_id,
               release_item.id as release_id,
               release_item.last_modified_at as observed_at
        from release_item
        where release_item.is_master is true
          and release_item.master_id is not null
        order by release_item.master_id,
                 release_item.last_modified_at desc,
                 release_item.id desc
      ),
      stale as materialized (
        select target.id,
               current_release.last_modified_at as observed_at
        from master target
        join release_item current_release on current_release.id = target.main_release_id
        left join desired on desired.master_id = target.id
        where target.main_release_id is distinct from desired.release_id
        order by target.id
        for update of target
      )
      update master target
      set main_release_id = null,
          last_modified_at = stale.observed_at
      from stale
      where target.id = stale.id
      """;

  static final String SET_CURRENT_MAPPINGS_SQL =
      """
      with desired as materialized (
        select distinct on (release_item.master_id)
               release_item.master_id,
               release_item.id as release_id,
               release_item.last_modified_at as observed_at
        from release_item
        where release_item.is_master is true
          and release_item.master_id is not null
        order by release_item.master_id,
                 release_item.last_modified_at desc,
                 release_item.id desc
      ),
      pending as materialized (
        select target.id,
               desired.release_id,
               desired.observed_at
        from desired
        join master target on target.id = desired.master_id
        where target.main_release_id is distinct from desired.release_id
        order by target.id
        for update of target
      )
      update master target
      set main_release_id = pending.release_id,
          last_modified_at = pending.observed_at
      from pending
      where target.id = pending.id
      """;

  private final JdbcTemplate jdbcTemplate;

  public PostgreSQLMasterMainReleaseReconciler(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Override
  public void reconcile() {
    jdbcTemplate.update(CLEAR_STALE_MAPPINGS_SQL);
    jdbcTemplate.update(SET_CURRENT_MAPPINGS_SQL);
  }
}
