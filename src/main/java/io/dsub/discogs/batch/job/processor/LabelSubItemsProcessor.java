package io.dsub.discogs.batch.job.processor;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.LABEL;

import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.LabelUrlRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jooq.UpdatableRecord;

public class LabelSubItemsProcessor
    implements ObservedAtItemProcessor<LabelSubItemsXML, RelationSet> {

  private final EntityIdRegistry idRegistry;

  public LabelSubItemsProcessor(EntityIdRegistry idRegistry) {
    this.idRegistry = Objects.requireNonNull(idRegistry, "idRegistry must not be null");
  }

  @Override
  public RelationSet process(LabelSubItemsXML item, LocalDateTime observedAt) {
    if (item.getId() == null || item.getId() < 1) {
      return null;
    }

    ReflectionUtil.normalizeStringFields(item);

    List<UpdatableRecord<?>> records = new ArrayList<>();

    Integer labelId = item.getId();

    if (item.getLabelSubLabels() != null) {
      item.getLabelSubLabels().stream()
          .filter(Objects::nonNull)
          .filter(subLabel -> isExistingLabel(subLabel.getSubLabelId()))
          .map(xml -> xml.getRecord(labelId, observedAt))
          .forEach(records::add);
    }

    if (item.getUrls() != null) {
      item.getUrls().stream()
          .filter(Objects::nonNull)
          .map(url -> getLabelUrlRecord(labelId, url, observedAt))
          .forEach(records::add);
    }

    return new RelationSet(EntityType.LABEL, labelId, records);
  }

  private LabelUrlRecord getLabelUrlRecord(
      Integer labelId, String url, LocalDateTime observedAt) {
    return new LabelUrlRecord()
        .setLabelId(labelId)
        .setUrl(url)
        .setHash(url.hashCode())
        .setLastModifiedAt(observedAt)
        .setCreatedAt(observedAt);
  }

  private boolean isExistingLabel(Integer labelId) {
    return idRegistry.exists(LABEL, labelId);
  }
}
