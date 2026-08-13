package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import java.time.LocalDateTime;

public class MasterCoreProcessor
    implements ObservedAtItemProcessor<MasterXML, MasterRecord> {

  @Override
  public MasterRecord process(MasterXML master, LocalDateTime observedAt) {
    if (master.getId() == null || master.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeStringFields(master);
    return master.buildRecord(observedAt);
  }
}
