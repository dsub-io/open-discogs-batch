package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.LabelRecord;
import java.time.LocalDateTime;

public class LabelCoreProcessor
    implements ObservedAtItemProcessor<LabelXML, LabelRecord> {

  @Override
  public LabelRecord process(LabelXML command, LocalDateTime observedAt) {
    if (command.getId() == null || command.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeStringFields(command);
    return command.buildRecord(observedAt);
  }
}
