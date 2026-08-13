package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import java.time.LocalDateTime;

public class ArtistCoreProcessor
    implements ObservedAtItemProcessor<ArtistXML, ArtistRecord> {

  @Override
  public ArtistRecord process(ArtistXML item, LocalDateTime observedAt) {
    if (item.getId() == null || item.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeStringFields(item);
    return item.buildRecord(observedAt);
  }
}
