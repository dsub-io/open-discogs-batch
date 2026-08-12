package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@RequiredArgsConstructor
public class MasterMainReleaseItemProcessor
    implements ItemProcessor<MasterMainReleaseXML, MasterMainReleaseAssignment> {

  private final EntityIdRegistry idRegistry;

  @Override
  public MasterMainReleaseAssignment process(MasterMainReleaseXML item) throws Exception {
    if (item == null
        || !idRegistry.exists(DefaultEntityIdRegistry.Type.RELEASE, item.getReleaseId())) {
      return null;
    }

    MasterMainReleaseXML.Master master = item.getMaster();
    if (master != null
        && master.isMainRelease()
        && !idRegistry.exists(DefaultEntityIdRegistry.Type.MASTER, master.getMasterId())) {
      return null;
    }

    return item.buildRecord();
  }
}
