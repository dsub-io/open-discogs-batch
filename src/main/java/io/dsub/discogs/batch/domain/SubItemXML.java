package io.dsub.discogs.batch.domain;

import java.time.LocalDateTime;
import org.jooq.UpdatableRecord;

public interface SubItemXML<T extends UpdatableRecord<T>> {

  T getRecord(int parentId, LocalDateTime observedAt);
}
