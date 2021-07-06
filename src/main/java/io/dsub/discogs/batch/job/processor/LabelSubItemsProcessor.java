package io.dsub.discogs.batch.job.processor;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.LABEL;

import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.jooq.tables.records.LabelUrlRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.UpdatableRecord;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class LabelSubItemsProcessor
    implements ItemProcessor<LabelSubItemsXML, Collection<UpdatableRecord<?>>> {

  private final EntityIdRegistry idRegistry;

  @Override
  public Collection<UpdatableRecord<?>> process(LabelSubItemsXML item) {
    if (item.getId() == null || item.getId() < 1) {
      return null;
    }

    ReflectionUtil.normalizeStringFields(item);

    List<UpdatableRecord<?>> records = new ArrayList<>();

    Integer labelId = item.getId();

    if (item.getLabelSubLabels() != null) {
      item.getLabelSubLabels().stream()
          .filter(subLabel -> isExistingLabel(subLabel.getSubLabelId()))
          .map(xml -> xml.getRecord(labelId))
          .forEach(records::add);
    }

    if (item.getUrls() != null) {
      item.getUrls().stream()
          .filter(url -> !url.isBlank())
          .distinct()
          .map(url -> getLabelUrlRecord(labelId, url))
          .forEach(records::add);
    }

    return records;
  }

  private LabelUrlRecord getLabelUrlRecord(Integer labelId, String url) {
    return new LabelUrlRecord()
        .setLabelId(labelId)
        .setUrl(url)
        .setHash(url.hashCode())
        .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()))
        .setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
  }

  private boolean isExistingLabel(Integer labelId) {
    return idRegistry.exists(LABEL, labelId);
  }
}
