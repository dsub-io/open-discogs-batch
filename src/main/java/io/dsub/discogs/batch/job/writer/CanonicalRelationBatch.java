package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jooq.Table;
import org.jooq.TableRecord;

/** Canonicalizes semantic identities before validating PostgreSQL storage identities. */
final class CanonicalRelationBatch {

  private CanonicalRelationBatch() {
  }

  static List<RelationSet> canonicalize(
      List<? extends RelationSet> relationSets, EntityType entityType) {
    return prepare(relationSets, entityType).relationSets();
  }

  static CanonicalBatch prepare(
      List<? extends RelationSet> relationSets, EntityType entityType) {
    ReleaseRelationSlotAllocator.assignCanonicalDigests(relationSets, entityType);
    List<RelationSet> semanticSets =
        collapseByIdentity(relationSets, entityType, IdentityKind.SEMANTIC);
    ReleaseRelationSlotAllocator.allocateAssignedDigests(semanticSets, entityType);
    List<RelationSet> physicalSets =
        collapseByIdentity(semanticSets, entityType, IdentityKind.PHYSICAL);

    Map<Table<?>, List<TableRecord<?>>> recordsByTable = new LinkedHashMap<>();
    physicalSets.stream()
        .flatMap(relationSet -> relationSet.records().stream())
        .forEach(
            record ->
                recordsByTable
                    .computeIfAbsent(record.getTable(), ignored -> new ArrayList<>())
                    .add(record));
    return new CanonicalBatch(physicalSets, recordsByTable);
  }

  private static List<RelationSet> collapseByIdentity(
      List<? extends RelationSet> relationSets,
      EntityType entityType,
      IdentityKind identityKind) {
    List<List<TableRecord<?>>> canonicalRecords = new ArrayList<>(relationSets.size());
    for (int index = 0; index < relationSets.size(); index++) {
      canonicalRecords.add(new ArrayList<>());
    }

    Map<RelationTableRegistry.RelationIdentity, CanonicalRecord> recordsByIdentity =
        new LinkedHashMap<>();
    for (int setIndex = 0; setIndex < relationSets.size(); setIndex++) {
      RelationSet relationSet = relationSets.get(setIndex);
      for (TableRecord<?> record : relationSet.records()) {
        RelationTableRegistry.RelationTable relationTable =
            RelationTableRegistry.require(entityType, record.getTable());
        relationTable.requireRoot(record, relationSet.rootId());
        RelationTableRegistry.RelationIdentity identity =
            identityKind.identity(relationTable, record);
        CanonicalRecord existing = recordsByIdentity.get(identity);
        if (existing == null) {
          recordsByIdentity.put(identity, new CanonicalRecord(record));
          canonicalRecords.get(setIndex).add(record);
        } else if (!relationTable.hasSamePayload(existing.record(), record)) {
          throw new IllegalArgumentException(
              "conflicting persisted payload for " + relationTable.table().getName()
                  + " (" + relationTable.describeSemanticIdentity(record) + ")");
        }
      }
    }

    List<RelationSet> canonicalSets = new ArrayList<>(relationSets.size());
    for (int index = 0; index < relationSets.size(); index++) {
      RelationSet source = relationSets.get(index);
      canonicalSets.add(
          new RelationSet(source.entityType(), source.rootId(), canonicalRecords.get(index)));
    }
    return List.copyOf(canonicalSets);
  }

  record CanonicalBatch(
      List<RelationSet> relationSets, Map<Table<?>, List<TableRecord<?>>> recordsByTable) {

    CanonicalBatch {
      relationSets = List.copyOf(relationSets);
      Map<Table<?>, List<TableRecord<?>>> immutableRecords = new LinkedHashMap<>();
      recordsByTable.forEach((table, records) -> immutableRecords.put(table, List.copyOf(records)));
      recordsByTable = Map.copyOf(immutableRecords);
    }

    List<TableRecord<?>> recordsFor(RelationTableRegistry.RelationTable relationTable) {
      return recordsByTable.getOrDefault(relationTable.table(), List.of());
    }
  }

  private record CanonicalRecord(TableRecord<?> record) {
  }

  private enum IdentityKind {
    SEMANTIC {
      @Override
      RelationTableRegistry.RelationIdentity identity(
          RelationTableRegistry.RelationTable relationTable, TableRecord<?> record) {
        return relationTable.semanticIdentity(record);
      }

    },
    PHYSICAL {
      @Override
      RelationTableRegistry.RelationIdentity identity(
          RelationTableRegistry.RelationTable relationTable, TableRecord<?> record) {
        return relationTable.identity(record);
      }

    };

    abstract RelationTableRegistry.RelationIdentity identity(
        RelationTableRegistry.RelationTable relationTable, TableRecord<?> record);

  }
}
