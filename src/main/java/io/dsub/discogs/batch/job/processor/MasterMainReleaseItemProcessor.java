package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@RequiredArgsConstructor
public class MasterMainReleaseItemProcessor
    implements ItemProcessor<MasterMainReleaseXML, MasterRecord> {

  private final EntityIdRegistry idRegistry;

  @Override
  public MasterRecord process(MasterMainReleaseXML item) throws Exception {
    if (item == null
        || item.getReleaseId() == null
        || item.getMaster() == null
        || item.getMaster().getMasterId() == null
        || !item.getMaster().isMainRelease()) {
      return null;
    }

    if (!idRegistry.exists(DefaultEntityIdRegistry.Type.MASTER, item.getMaster().getMasterId())
        || !idRegistry.exists(DefaultEntityIdRegistry.Type.RELEASE, item.getReleaseId())) {
      return null;
    }

    return item.buildRecord();
  }
}
