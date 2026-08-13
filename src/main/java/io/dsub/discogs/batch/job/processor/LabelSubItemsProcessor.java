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

    List<String> sourceUrls = item.getUrls();
    ReflectionUtil.normalizeStringFields(item);

    List<UpdatableRecord<?>> records = new ArrayList<>();

    Integer labelId = item.getId();

    if (item.getLabelSubLabels() != null) {
      for (int ordinal = 0; ordinal < item.getLabelSubLabels().size(); ordinal++) {
        LabelSubItemsXML.LabelSubLabelXML subLabel = item.getLabelSubLabels().get(ordinal);
        if (subLabel != null && isExistingLabel(subLabel.getSubLabelId())) {
          records.add(subLabel.getRecord(labelId, observedAt).setOrdinal(ordinal));
        }
      }
    }

    if (sourceUrls != null) {
      for (int ordinal = 0; ordinal < sourceUrls.size(); ordinal++) {
        String sourceUrl = sourceUrls.get(ordinal);
        String url = sourceUrl == null ? null : sourceUrl.trim();
        if (url != null && url.isBlank()) {
          url = null;
        }
        if (url != null) {
          records.add(getLabelUrlRecord(labelId, url, ordinal, observedAt));
        }
      }
    }

    return new RelationSet(EntityType.LABEL, labelId, records);
  }

  private LabelUrlRecord getLabelUrlRecord(
      Integer labelId, String url, int ordinal, LocalDateTime observedAt) {
    return new LabelUrlRecord()
        .setLabelId(labelId)
        .setOrdinal(ordinal)
        .setUrl(url)
        .setHash(url.hashCode())
        .setLastModifiedAt(observedAt);
  }

  private boolean isExistingLabel(Integer labelId) {
    return idRegistry.exists(LABEL, labelId);
  }
}
