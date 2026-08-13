package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

/** Validates and collapses relation rows by their canonical PostgreSQL conflict target. */
final class CanonicalRelationBatch {

  private CanonicalRelationBatch() {
  }

  static List<RelationSet> canonicalize(
      List<? extends RelationSet> relationSets, EntityType entityType) {
    return prepare(relationSets, entityType).relationSets();
  }

  static CanonicalBatch prepare(
      List<? extends RelationSet> relationSets, EntityType entityType) {
    ReleaseRelationSlotAllocator.allocate(relationSets, entityType);
    List<List<UpdatableRecord<?>>> canonicalRecords = new ArrayList<>(relationSets.size());
    for (int index = 0; index < relationSets.size(); index++) {
      canonicalRecords.add(new ArrayList<>());
    }

    Map<RelationTableRegistry.RelationIdentity, CanonicalRecord> recordsByIdentity =
        new LinkedHashMap<>();
    Map<Table<?>, List<UpdatableRecord<?>>> recordsByTable = new LinkedHashMap<>();
    for (int setIndex = 0; setIndex < relationSets.size(); setIndex++) {
      RelationSet relationSet = relationSets.get(setIndex);
      for (UpdatableRecord<?> record : relationSet.records()) {
        RelationTableRegistry.RelationTable relationTable =
            RelationTableRegistry.require(entityType, record.getTable());
        relationTable.requireRoot(record, relationSet.rootId());
        RelationTableRegistry.RelationIdentity identity = relationTable.identity(record);
        CanonicalRecord existing = recordsByIdentity.get(identity);
        if (existing == null) {
          recordsByIdentity.put(identity, new CanonicalRecord(record));
          canonicalRecords.get(setIndex).add(record);
          recordsByTable
              .computeIfAbsent(record.getTable(), ignored -> new ArrayList<>())
              .add(record);
        } else if (!relationTable.hasSamePayload(existing.record(), record)) {
          throw new IllegalArgumentException(
              "conflicting persisted payload for " + relationTable.table().getName()
                  + " (" + relationTable.describeIdentity(record) + ")");
        }
      }
    }

    List<RelationSet> canonicalSets = new ArrayList<>(relationSets.size());
    for (int index = 0; index < relationSets.size(); index++) {
      RelationSet source = relationSets.get(index);
      canonicalSets.add(
          new RelationSet(source.entityType(), source.rootId(), canonicalRecords.get(index)));
    }
    return new CanonicalBatch(canonicalSets, recordsByTable);
  }

  record CanonicalBatch(
      List<RelationSet> relationSets, Map<Table<?>, List<UpdatableRecord<?>>> recordsByTable) {

    CanonicalBatch {
      relationSets = List.copyOf(relationSets);
      Map<Table<?>, List<UpdatableRecord<?>>> immutableRecords = new LinkedHashMap<>();
      recordsByTable.forEach((table, records) -> immutableRecords.put(table, List.copyOf(records)));
      recordsByTable = Map.copyOf(immutableRecords);
    }

    List<UpdatableRecord<?>> recordsFor(RelationTableRegistry.RelationTable relationTable) {
      return recordsByTable.getOrDefault(relationTable.table(), List.of());
    }
  }

  private record CanonicalRecord(UpdatableRecord<?> record) {
  }
}
