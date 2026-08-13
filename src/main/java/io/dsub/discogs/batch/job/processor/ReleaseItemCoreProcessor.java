package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.DefaultMalformedDateParser;
import io.dsub.discogs.batch.util.MalformedDateParser;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class ReleaseItemCoreProcessor implements ItemProcessor<ReleaseItemXML, ReleaseItemRecord> {

  private final MalformedDateParser parser = new DefaultMalformedDateParser();
  private final EntityIdRegistry idRegistry;
  private final Clock clock;

  public ReleaseItemCoreProcessor(EntityIdRegistry idRegistry) {
    this(idRegistry, Clock.systemUTC());
  }

  ReleaseItemCoreProcessor(EntityIdRegistry idRegistry, Clock clock) {
    this.idRegistry = Objects.requireNonNull(idRegistry, "idRegistry must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ReleaseItemRecord process(ReleaseItemXML release) throws Exception {

    if (!hasValidId(release.getId())) {
      return null;
    }

    ReflectionUtil.normalizeReleaseStringFields(release);
    return processNormalized(release, LocalDateTime.now(clock));
  }

  ReleaseItemRecord processNormalized(ReleaseItemXML release, LocalDateTime observedAt) {
    Integer masterId = existingMasterId(
        release.getMaster() == null ? null : release.getMaster().getMasterId());
    return buildRecord(
        release.getId(),
        release.getTitle(),
        release.getStatus(),
        release.getCountry(),
        release.getDataQuality(),
        release.getReleaseDate(),
        release.getMaster() != null && release.getMaster().isMaster(),
        masterId,
        release.getNotes(),
        observedAt);
  }

  ReleaseItemRecord processNormalized(
      ReleaseItemSubItemsXML release, LocalDateTime observedAt) {
    Integer masterId = existingMasterId(
        release.getMaster() == null ? null : release.getMaster().getMasterId());
    return buildRecord(
        release.getId(),
        release.getTitle(),
        release.getStatus(),
        release.getCountry(),
        release.getDataQuality(),
        release.getReleaseDate(),
        release.getMaster() != null && release.getMaster().isMainRelease(),
        masterId,
        release.getNotes(),
        observedAt);
  }

  private Integer existingMasterId(Integer id) {
    if (id != null && idRegistry.exists(DefaultEntityIdRegistry.Type.MASTER, id)) {
      return id;
    }
    return null;
  }

  private ReleaseItemRecord buildRecord(
      Integer id,
      String title,
      String status,
      String country,
      String dataQuality,
      String releaseDate,
      boolean isMaster,
      Integer masterId,
      String notes,
      LocalDateTime observedAt) {
    return new ReleaseItemRecord()
        .setId(id)
        .setTitle(title)
        .setStatus(status)
        .setCountry(country)
        .setDataQuality(dataQuality)
        .setReleaseDate(parser.parse(releaseDate))
        .setHasValidDay(parser.isDayValid(releaseDate))
        .setHasValidMonth(parser.isMonthValid(releaseDate))
        .setHasValidYear(parser.isYearValid(releaseDate))
        .setListedReleaseDate(releaseDate)
        .setIsMaster(isMaster)
        .setMasterId(masterId)
        .setNotes(notes)
        .setCreatedAt(observedAt)
        .setLastModifiedAt(observedAt);
  }

  private boolean hasValidId(Integer id) {
    return id != null && id > 0;
  }
}
