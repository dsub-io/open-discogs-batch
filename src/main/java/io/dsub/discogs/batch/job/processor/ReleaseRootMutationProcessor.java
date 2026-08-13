package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.GenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.StyleRecord;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Builds the root, relations, and main-release assignment from one XML element. */
public final class ReleaseRootMutationProcessor
    implements ObservedAtItemProcessor<ReleaseItemSubItemsXML, ReleaseRootMutation> {

  private final ReleaseItemCoreProcessor coreProcessor;
  private final ReleaseItemSubItemsProcessor relationProcessor;

  public ReleaseRootMutationProcessor(EntityIdRegistry idRegistry) {
    this.coreProcessor = new ReleaseItemCoreProcessor(idRegistry);
    this.relationProcessor = new ReleaseItemSubItemsProcessor(idRegistry);
  }

  @Override
  public ReleaseRootMutation process(ReleaseItemSubItemsXML source, LocalDateTime observedAt) {
    if (source == null || source.getId() == null || source.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeReleaseStringFields(source);
    List<String> genres = normalizedValues(source.getGenres());
    List<String> styles = normalizedValues(source.getStyles());
    source.setGenres(genres);
    source.setStyles(styles);
    ReleaseItemRecord root = coreProcessor.processNormalized(source, observedAt);
    RelationSet relations = relationProcessor.processNormalized(source, observedAt);
    return new ReleaseRootMutation(
        root,
        genres.stream().map(value -> new GenreRecord().setName(value)).toList(),
        styles.stream().map(value -> new StyleRecord().setName(value)).toList(),
        relations);
  }

  private List<String> normalizedValues(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return List.copyOf(new LinkedHashSet<>(values));
  }
}
