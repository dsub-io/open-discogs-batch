package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.records.GenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.StyleRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/** Builds the root, relations, and main-release assignment from one XML element. */
public final class ReleaseRootMutationProcessor
    implements ItemProcessor<ReleaseItemSubItemsXML, ReleaseRootMutation> {

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
    if (source == null || source.getId() == null || source.getId() < 1) {
      return null;
    }
    ReleaseItemRecord root = coreProcessor.process(coreSource(source));
    List<String> genres = normalizedValues(source.getGenres());
    List<String> styles = normalizedValues(source.getStyles());
    RelationSet relations = relationProcessor.process(source);
    LocalDateTime observedAt = LocalDateTime.now(clock);
    Integer targetMasterId = root.getIsMaster() ? root.getMasterId() : null;
    return new ReleaseRootMutation(
        root,
        genres.stream().map(value -> new GenreRecord().setName(value)).toList(),
        styles.stream().map(value -> new StyleRecord().setName(value)).toList(),
        relations,
        new MasterMainReleaseAssignment(source.getId(), targetMasterId, observedAt));
  }

  private ReleaseItemXML coreSource(ReleaseItemSubItemsXML source) {
    ReleaseItemXML core = new ReleaseItemXML();
    core.setId(source.getId());
    core.setStatus(source.getStatus());
    core.setTitle(source.getTitle());
    core.setCountry(source.getCountry());
    core.setNotes(source.getNotes());
    core.setDataQuality(source.getDataQuality());
    core.setReleaseDate(source.getReleaseDate());
    core.setGenres(source.getGenres());
    core.setStyles(source.getStyles());
    if (source.getMaster() != null) {
      ReleaseItemXML.Master master = new ReleaseItemXML.Master();
      master.setMasterId(source.getMaster().getMasterId());
      master.setMaster(source.getMaster().isMainRelease());
      core.setMaster(master);
    }
    return core;
  }

  private List<String> normalizedValues(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .distinct()
        .toList();
  }
}
