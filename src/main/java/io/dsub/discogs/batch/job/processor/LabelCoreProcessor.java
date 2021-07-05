package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.jooq.tables.records.LabelRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class LabelCoreProcessor implements ItemProcessor<LabelXML, LabelRecord> {

  @Override
  public LabelRecord process(LabelXML command) throws Exception {
    if (command.getId() == null || command.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeStringFields(command);
    return command.buildRecord();
  }
}
