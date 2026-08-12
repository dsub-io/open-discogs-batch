package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Reads relation roots that need stale-row reconciliation for one chunk. */
final class ExistingRelationRootsReader {

  private static final String INTEGER_ARRAY_TYPE = "integer";
  private static final String ROOT_ID_COLUMN = "root_id";
  private static final String TABLE_NAME_COLUMN = "relation_table";
  private static final String ROOTS_CTE =
      "with incoming_roots(root_id) as (select unnest(?::integer[])) ";
  private static final String UNION = " union all ";

  private final JdbcTemplate jdbcTemplate;

  ExistingRelationRootsReader(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  ExistingRelationRoots find(EntityType entityType, Set<Integer> rootIds) {
    if (rootIds.isEmpty()) {
      return new ExistingRelationRoots(Map.of());
    }
    List<RelationTableRegistry.RelationTable> relationTables =
        RelationTableRegistry.forEntity(entityType);
    String sql =
        ROOTS_CTE
            + relationTables.stream()
                .map(RelationTableRegistry.RelationTable::existingRootsSelectSql)
                .reduce((left, right) -> left + UNION + right)
                .orElseThrow();

    return jdbcTemplate.execute(
        (Connection connection) -> {
          Array roots = connection.createArrayOf(INTEGER_ARRAY_TYPE, rootIds.toArray());
          try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
              statement.setArray(1, roots);
              try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, Set<Integer>> rootIdsByTable = new HashMap<>();
                while (resultSet.next()) {
                  rootIdsByTable
                      .computeIfAbsent(
                          resultSet.getString(TABLE_NAME_COLUMN),
                          ignored -> new LinkedHashSet<>())
                      .add(resultSet.getInt(ROOT_ID_COLUMN));
                }
                return new ExistingRelationRoots(rootIdsByTable);
              }
            }
          } finally {
            roots.free();
          }
        });
  }
}
