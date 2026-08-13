package io.dsub.discogs.batch.job.processor;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.ARTIST;
import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.GENRE;
import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.STYLE;

import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.MasterGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.MasterStyleRecord;
import io.dsub.opendiscogs.jooq.tables.records.MasterVideoRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jooq.UpdatableRecord;

public class MasterSubItemsProcessor
    implements ObservedAtItemProcessor<MasterSubItemsXML, RelationSet> {

  private final EntityIdRegistry idRegistry;

  public MasterSubItemsProcessor(EntityIdRegistry idRegistry) {
    this.idRegistry = Objects.requireNonNull(idRegistry, "idRegistry must not be null");
  }

  @Override
  public RelationSet process(MasterSubItemsXML master, LocalDateTime observedAt) {

    if (master.getId() == null || master.getId() < 1) {
      return null;
    }

    ReflectionUtil.normalizeStringFields(master);

    List<UpdatableRecord<?>> items = new ArrayList<>();
    Integer masterId = master.getId();

    if (master.getMasterArtists() != null) {
      master.getMasterArtists().stream()
          .filter(Objects::nonNull)
          .filter(masterArtist -> isExistingArtist(masterArtist.getArtistId()))
          .map(xml -> xml.getRecord(masterId, observedAt))
          .forEach(items::add);
    }

    if (master.getMasterVideos() != null) {
      master.getMasterVideos().stream()
          .filter(Objects::nonNull)
          .filter(video -> video.getUrl() != null)
          .map(video -> getMasterVideoRecord(masterId, video, observedAt))
          .forEach(items::add);
    }

    if (master.getGenres() != null) {
      master.getGenres().stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(this::isExistingGenre)
          .map(genre -> getMasterGenreRecord(masterId, genre, observedAt))
          .forEach(items::add);
    }

    if (master.getStyles() != null) {
      master.getStyles().stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(this::isExistingStyle)
          .map(style -> getMasterStyleRecord(masterId, style, observedAt))
          .forEach(items::add);
    }

    return new RelationSet(
        EntityType.MASTER, masterId, items);
  }

  private boolean isExistingArtist(Integer id) {
    return idRegistry.exists(ARTIST, id);
  }

  private boolean isExistingStyle(String name) {
    return idRegistry.exists(STYLE, name);
  }

  private boolean isExistingGenre(String name) {
    return idRegistry.exists(GENRE, name);
  }

  private MasterGenreRecord getMasterGenreRecord(
      Integer masterId, String genre, LocalDateTime observedAt) {
    return new MasterGenreRecord()
        .setMasterId(masterId)
        .setGenre(genre)
        .setLastModifiedAt(observedAt);
  }

  private MasterStyleRecord getMasterStyleRecord(
      Integer masterId, String style, LocalDateTime observedAt) {
    return new MasterStyleRecord()
        .setMasterId(masterId)
        .setStyle(style)
        .setLastModifiedAt(observedAt);
  }

  private MasterVideoRecord getMasterVideoRecord(
      Integer masterId, MasterSubItemsXML.MasterVideoXML video, LocalDateTime observedAt) {
    String hashSrc =
        (video.getTitle() == null ? "" : video.getTitle())
            + (video.getDescription() == null ? "" : video.getDescription())
            + video.getUrl();
    return new MasterVideoRecord()
        .setMasterId(masterId)
        .setTitle(video.getTitle())
        .setDescription(video.getDescription())
        .setUrl(video.getUrl())
        .setHash(hashSrc.hashCode())
        .setLastModifiedAt(observedAt);
  }
}
