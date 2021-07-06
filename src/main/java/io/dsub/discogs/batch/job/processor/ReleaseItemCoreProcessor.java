package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.DefaultMalformedDateParser;
import io.dsub.discogs.batch.util.MalformedDateParser;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.jooq.tables.records.ReleaseItemRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class ReleaseItemCoreProcessor implements ItemProcessor<ReleaseItemXML, ReleaseItemRecord> {

  private final MalformedDateParser parser = new DefaultMalformedDateParser();
  private final EntityIdRegistry idRegistry;

  @Override
  public ReleaseItemRecord process(ReleaseItemXML release) throws Exception {

    if (release.getId() == null || release.getId() < 1) {
      return null;
    }

    ReflectionUtil.normalizeStringFields(release);

    Integer masterId = null;

    if (release.getMaster() != null && release.getMaster().getMasterId() != null) {
      Integer id = release.getMaster().getMasterId();
      if (idRegistry.exists(EntityIdRegistry.Type.MASTER, id)) {
        masterId = id;
      }
    }

    return new ReleaseItemRecord()
        .setId(release.getId())
        .setTitle(release.getTitle())
        .setStatus(release.getStatus())
        .setCountry(release.getCountry())
        .setDataQuality(release.getDataQuality())
        .setReleaseDate(parser.parse(release.getReleaseDate()))
        .setHasValidDay(parser.isDayValid(release.getReleaseDate()))
        .setHasValidMonth(parser.isMonthValid(release.getReleaseDate()))
        .setHasValidYear(parser.isYearValid(release.getReleaseDate()))
        .setListedReleaseDate(release.getReleaseDate())
        .setIsMaster(release.getMaster() != null && release.getMaster().isMaster())
        .setMasterId(masterId)
        .setNotes(release.getNotes())
        .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
        .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
  }
}
