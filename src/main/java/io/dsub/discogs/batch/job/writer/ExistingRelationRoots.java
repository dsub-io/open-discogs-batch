package io.dsub.discogs.batch.job.writer;

import java.util.Map;
import java.util.Set;

/** Existing relation roots grouped by their canonical relation table. */
record ExistingRelationRoots(Map<String, Set<Integer>> rootIdsByTable) {

  ExistingRelationRoots {
    rootIdsByTable = Map.copyOf(rootIdsByTable);
  }

  Set<Integer> forTable(RelationTableRegistry.RelationTable relationTable) {
    return rootIdsByTable.getOrDefault(relationTable.table().getName(), Set.of());
  }
}
