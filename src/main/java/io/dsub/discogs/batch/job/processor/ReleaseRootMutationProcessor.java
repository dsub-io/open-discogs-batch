package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.GenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.StyleRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/** Builds the root, relations, and main-release assignment from one XML element. */
public final class ReleaseRootMutationProcessor
    implements ItemProcessor<ReleaseItemSubItemsXML, ReleaseRootMutation>,
        ObservedAtItemProcessor<ReleaseItemSubItemsXML, ReleaseRootMutation> {

  private final ReleaseItemCoreProcessor coreProcessor;
  private final ReleaseItemSubItemsProcessor relationProcessor;
  private final Clock clock;

  public ReleaseRootMutationProcessor(EntityIdRegistry idRegistry) {
    this(idRegistry, Clock.systemUTC());
  }

  ReleaseRootMutationProcessor(EntityIdRegistry idRegistry, Clock clock) {
    this.coreProcessor = new ReleaseItemCoreProcessor(idRegistry);
    this.relationProcessor = new ReleaseItemSubItemsProcessor(idRegistry);
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ReleaseRootMutation process(ReleaseItemSubItemsXML source) throws Exception {
    return process(source, LocalDateTime.now(clock));
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
    Integer targetMasterId = root.getIsMaster() ? root.getMasterId() : null;
    return new ReleaseRootMutation(
        root,
        genres.stream().map(value -> new GenreRecord().setName(value)).toList(),
        styles.stream().map(value -> new StyleRecord().setName(value)).toList(),
        relations,
        new MasterMainReleaseAssignment(source.getId(), targetMasterId, observedAt));
  }

  private List<String> normalizedValues(List<String> values) {
    if (values == null) {
      return List.of();
    }
    LinkedHashSet<String> uniqueValues = new LinkedHashSet<>();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        uniqueValues.add(value);
      }
    }
    return List.copyOf(uniqueValues);
  }
}
