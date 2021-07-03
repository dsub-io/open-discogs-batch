package io.dsub.discogs.batch.domain;

import org.jooq.UpdatableRecord;

import java.time.Clock;
import java.time.LocalDateTime;

public interface BaseXML<T extends UpdatableRecord<T>> {
    T buildRecord();
}