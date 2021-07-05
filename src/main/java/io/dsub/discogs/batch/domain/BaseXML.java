package io.dsub.discogs.batch.domain;

import org.jooq.UpdatableRecord;

public interface BaseXML<T extends UpdatableRecord<T>> {

  T buildRecord();
}
