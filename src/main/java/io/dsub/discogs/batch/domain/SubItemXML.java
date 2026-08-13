package io.dsub.discogs.batch.domain;

import java.time.LocalDateTime;
import org.jooq.TableRecord;

public interface SubItemXML<T extends TableRecord<T>> {

  T getRecord(int parentId, LocalDateTime observedAt);
}
