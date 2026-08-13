package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import java.time.LocalDateTime;
import java.util.Objects;

public class MasterMainReleaseItemProcessor
    implements ObservedAtItemProcessor<MasterMainReleaseXML, MasterMainReleaseAssignment> {

  private final EntityIdRegistry idRegistry;

  public MasterMainReleaseItemProcessor(EntityIdRegistry idRegistry) {
    this.idRegistry = Objects.requireNonNull(idRegistry, "idRegistry must not be null");
  }

  @Override
  public MasterMainReleaseAssignment process(
      MasterMainReleaseXML item, LocalDateTime observedAt) {
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

    return item.buildRecord(observedAt);
  }
}
