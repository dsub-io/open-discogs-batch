package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import java.time.LocalDateTime;

public class MasterCoreProcessor
    implements ObservedAtItemProcessor<MasterXML, MasterRecord> {

  private final boolean seedMainRelease;

  public MasterCoreProcessor() {
    this(false);
  }

  public MasterCoreProcessor(boolean seedMainRelease) {
    this.seedMainRelease = seedMainRelease;
  }

  @Override
  public MasterRecord process(MasterXML master, LocalDateTime observedAt) {
    if (master.getId() == null || master.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeStringFields(master);
    MasterRecord record = master.buildRecord(observedAt);
    if (!seedMainRelease) {
      record.setMainReleaseId(null);
    }
    return record;
  }
}
