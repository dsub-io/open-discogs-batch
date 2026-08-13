package io.dsub.discogs.batch.domain;

import java.time.LocalDateTime;

public interface BaseXML<T> {

  T buildRecord(LocalDateTime observedAt);
}
