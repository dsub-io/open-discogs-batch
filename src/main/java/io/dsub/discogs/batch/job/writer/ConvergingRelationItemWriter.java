package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.jooq.TableRecord;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

/** Replaces every relation set represented by roots in the current chunk. */
public class ConvergingRelationItemWriter implements ItemWriter<RelationSet> {

  private final JdbcTemplate jdbcTemplate;
  private final ExistingRelationRootsReader existingRelationRootsReader;
  private final ItemWriter<Collection<TableRecord<?>>> delegate;

  public ConvergingRelationItemWriter(
      DataSource dataSource,
      ItemWriter<Collection<TableRecord<?>>> delegate) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.existingRelationRootsReader = new ExistingRelationRootsReader(dataSource);
    this.delegate = delegate;
  }

  @Override
  public void write(Chunk<? extends RelationSet> items) throws Exception {
    if (items.isEmpty()) {
      return;
    }
    EntityType entityType = items.getItems().getFirst().entityType();
    Set<Integer> rootIds = new LinkedHashSet<>(items.size());
    for (RelationSet relationSet : items) {
      if (relationSet.entityType() != entityType) {
        throw new IllegalArgumentException("one chunk cannot contain multiple entity types");
      }
      rootIds.add(relationSet.rootId());
    }
    CanonicalRelationBatch.CanonicalBatch canonicalBatch =
        CanonicalRelationBatch.prepare(items.getItems(), entityType);
    ExistingRelationRoots existingRoots = existingRelationRootsReader.find(entityType, rootIds);

    for (RelationTableRegistry.RelationTable relationTable :
        RelationTableRegistry.forEntity(entityType)) {
      Set<Integer> existingRootIds = existingRoots.forTable(relationTable);
      if (existingRootIds.isEmpty()) {
        continue;
      }
      List<TableRecord<?>> currentRecords =
          canonicalBatch.recordsFor(relationTable).stream()
              .filter(record -> existingRootIds.contains(relationTable.rootId(record)))
              .toList();
      deleteStaleRelations(relationTable, existingRootIds, currentRecords);
    }

    List<Collection<TableRecord<?>>> records =
        canonicalBatch.relationSets().stream()
            .map(RelationSet::records)
            .map(recordsForRoot -> (Collection<TableRecord<?>>) recordsForRoot)
            .toList();
    delegate.write(new Chunk<>(records));
  }

  private void deleteStaleRelations(
      RelationTableRegistry.RelationTable relationTable,
      Set<Integer> rootIds,
      List<TableRecord<?>> currentRecords) {
    jdbcTemplate.execute(
        (Connection connection) -> {
          List<Array> arrays = new ArrayList<>(relationTable.keys().size() + 1);
          try {
            Array roots = connection.createArrayOf("integer", rootIds.toArray());
            arrays.add(roots);
            String sql =
                currentRecords.isEmpty()
                    ? relationTable.deleteAllForRootsSql()
                    : relationTable.deleteStaleSql(currentRecords.size());
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
              statement.setArray(1, roots);
              if (!currentRecords.isEmpty()) {
                int parameterIndex = 2;
                for (TableRecord<?> record : currentRecords) {
                  for (RelationTableRegistry.RelationKey key : relationTable.keys()) {
                    key.bind(statement, parameterIndex, record);
                    parameterIndex++;
                  }
                }
              }
              statement.executeUpdate();
            }
          } finally {
            for (Array array : arrays) {
              array.free();
            }
          }
          return null;
        });
  }
}
