package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class DefaultJooqMasterMainReleaseItemWriter
    implements ItemWriter<MasterMainReleaseAssignment> {

  static final String LOCK_MASTER_ROWS_SQL =
      """
      with candidate_master_ids as (
        select unnest(?::integer[]) as id
        union
        select current.id
        from master current
        where current.main_release_id = any(?::integer[])
      )
      select target.id
      from master target
      join candidate_master_ids candidate on candidate.id = target.id
      order by target.id
      for update of target
      """;
  static final String CLEAR_STALE_MAPPINGS_SQL =
      """
      update master target
      set main_release_id = null,
          last_modified_at = incoming.observed_at
      from unnest(?::integer[], ?::integer[], ?::timestamp[])
          as incoming(release_id, target_master_id, observed_at)
      where target.main_release_id = incoming.release_id
        and (incoming.target_master_id is null or target.id <> incoming.target_master_id)
      """;
  static final String SET_CURRENT_MAPPINGS_SQL =
      """
      update master target
      set main_release_id = incoming.release_id,
          last_modified_at = incoming.observed_at
      from unnest(?::integer[], ?::integer[], ?::timestamp[])
          as incoming(master_id, release_id, observed_at)
      where target.id = incoming.master_id
        and target.main_release_id is distinct from incoming.release_id
      """;
  private static final String INVALID_CONNECTION_PROVIDER_MESSAGE =
      "master main release writer requires a DataSource-backed DSLContext";
  private static final String MUTATION_FAILURE_MESSAGE =
      "failed to update master main release mappings";

  private final DSLContext context;

  public DefaultJooqMasterMainReleaseItemWriter(DSLContext context) {
    this.context = context;
  }

  @Override
  public void write(Chunk<? extends MasterMainReleaseAssignment> items) {
    if (items.isEmpty()) {
      return;
    }

    DataSource dataSource = dataSource(context.configuration().connectionProvider());
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      DSLContext transactionContext = DSL.using(context.configuration().derive(connection));
      List<MasterMainReleaseAssignment> assignments = new ArrayList<>(items.getItems());
      assignments.sort(Comparator.comparingInt(MasterMainReleaseAssignment::releaseId));
      requireDistinctTargetMasters(assignments);
      lockMasterRows(transactionContext, assignments);
      try {
        clearStaleMappings(connection, assignments);
        setCurrentMappings(connection, assignments);
      } catch (SQLException exception) {
        throw new DataAccessResourceFailureException(MUTATION_FAILURE_MESSAGE, exception);
      }
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void lockMasterRows(
      DSLContext transactionContext, List<MasterMainReleaseAssignment> assignments) {
    Integer[] releaseIds =
        assignments.stream()
            .map(MasterMainReleaseAssignment::releaseId)
            .distinct()
            .sorted()
            .toArray(Integer[]::new);
    Integer[] masterIds =
        assignments.stream()
            .map(MasterMainReleaseAssignment::targetMasterId)
            .filter(masterId -> masterId != null)
            .distinct()
            .sorted()
            .toArray(Integer[]::new);
    transactionContext.fetch(LOCK_MASTER_ROWS_SQL, masterIds, releaseIds);
  }

  private void requireDistinctTargetMasters(List<MasterMainReleaseAssignment> assignments) {
    Set<Integer> targetMasterIds = new HashSet<>();
    for (MasterMainReleaseAssignment assignment : assignments) {
      Integer targetMasterId = assignment.targetMasterId();
      if (targetMasterId != null && !targetMasterIds.add(targetMasterId)) {
        throw new IllegalArgumentException(
            "multiple main releases target master " + targetMasterId + " in one source chunk");
      }
    }
  }

  private void clearStaleMappings(
      Connection connection, List<MasterMainReleaseAssignment> assignments) throws SQLException {
    Object[] releaseIds = assignments.stream().map(MasterMainReleaseAssignment::releaseId).toArray();
    Object[] targetMasterIds =
        assignments.stream().map(MasterMainReleaseAssignment::targetMasterId).toArray();
    Object[] observedAt =
        assignments.stream().map(MasterMainReleaseAssignment::observedAt).toArray();
    executeArrayUpdate(
        connection,
        CLEAR_STALE_MAPPINGS_SQL,
        new SqlArray("integer", releaseIds),
        new SqlArray("integer", targetMasterIds),
        new SqlArray("timestamp", observedAt));
  }

  private void setCurrentMappings(
      Connection connection, List<MasterMainReleaseAssignment> assignments) throws SQLException {
    List<MasterMainReleaseAssignment> targeted =
        assignments.stream().filter(assignment -> assignment.targetMasterId() != null).toList();
    if (targeted.isEmpty()) {
      return;
    }
    Object[] masterIds = targeted.stream().map(MasterMainReleaseAssignment::targetMasterId).toArray();
    Object[] releaseIds = targeted.stream().map(MasterMainReleaseAssignment::releaseId).toArray();
    Object[] observedAt = targeted.stream().map(MasterMainReleaseAssignment::observedAt).toArray();
    executeArrayUpdate(
        connection,
        SET_CURRENT_MAPPINGS_SQL,
        new SqlArray("integer", masterIds),
        new SqlArray("integer", releaseIds),
        new SqlArray("timestamp", observedAt));
  }

  private void executeArrayUpdate(Connection connection, String sql, SqlArray... inputs)
      throws SQLException {
    List<Array> arrays = new ArrayList<>(inputs.length);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < inputs.length; index++) {
        SqlArray input = inputs[index];
        Array array = connection.createArrayOf(input.postgresType(), input.values());
        arrays.add(array);
        statement.setArray(index + 1, array);
      }
      statement.executeUpdate();
    } finally {
      for (Array array : arrays) {
        array.free();
      }
    }
  }

  private static DataSource dataSource(ConnectionProvider connectionProvider) {
    if (connectionProvider instanceof DataSourceConnectionProvider provider) {
      return provider.dataSource();
    }
    throw new IllegalArgumentException(INVALID_CONNECTION_PROVIDER_MESSAGE);
  }

  private record SqlArray(String postgresType, Object[] values) {
  }
}
