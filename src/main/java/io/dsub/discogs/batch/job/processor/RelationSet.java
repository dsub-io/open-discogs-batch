package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.dump.EntityType;
import java.util.List;
import org.jooq.TableRecord;

/** All currently present relation rows for one dump root. */
public record RelationSet(
    EntityType entityType, int rootId, List<TableRecord<?>> records) {

  public RelationSet {
    if (entityType == null) {
      throw new IllegalArgumentException("relation entity type must not be null");
    }
    if (rootId < 1) {
      throw new IllegalArgumentException("relation root ID must be positive");
    }
    if (records == null) {
      throw new IllegalArgumentException("relation records must not be null");
    }
    records = List.copyOf(records);
  }
}
