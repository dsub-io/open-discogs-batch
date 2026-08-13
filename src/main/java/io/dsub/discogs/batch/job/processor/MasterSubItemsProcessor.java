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

    List<String> sourceGenres = master.getGenres();
    List<String> sourceStyles = master.getStyles();
    ReflectionUtil.normalizeStringFields(master);

    List<UpdatableRecord<?>> items = new ArrayList<>();
    Integer masterId = master.getId();

    if (master.getMasterArtists() != null) {
      for (int ordinal = 0; ordinal < master.getMasterArtists().size(); ordinal++) {
        MasterSubItemsXML.MasterArtistXML artist = master.getMasterArtists().get(ordinal);
        if (artist != null && isExistingArtist(artist.getArtistId())) {
          items.add(artist.getRecord(masterId, observedAt).setOrdinal(ordinal));
        }
      }
    }

    if (master.getMasterVideos() != null) {
      for (int ordinal = 0; ordinal < master.getMasterVideos().size(); ordinal++) {
        MasterSubItemsXML.MasterVideoXML video = master.getMasterVideos().get(ordinal);
        if (video != null && video.getUrl() != null) {
          items.add(getMasterVideoRecord(masterId, video, ordinal, observedAt));
        }
      }
    }

    if (sourceGenres != null) {
      for (int ordinal = 0; ordinal < sourceGenres.size(); ordinal++) {
        String sourceGenre = sourceGenres.get(ordinal);
        if (sourceGenre == null) {
          continue;
        }
        String genre = sourceGenre.trim();
        if (isExistingGenre(genre)) {
          items.add(getMasterGenreRecord(masterId, genre, ordinal, observedAt));
        }
      }
    }

    if (sourceStyles != null) {
      for (int ordinal = 0; ordinal < sourceStyles.size(); ordinal++) {
        String sourceStyle = sourceStyles.get(ordinal);
        if (sourceStyle == null) {
          continue;
        }
        String style = sourceStyle.trim();
        if (isExistingStyle(style)) {
          items.add(getMasterStyleRecord(masterId, style, ordinal, observedAt));
        }
      }
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
      Integer masterId, String genre, int ordinal, LocalDateTime observedAt) {
    return new MasterGenreRecord()
        .setMasterId(masterId)
        .setOrdinal(ordinal)
        .setGenre(genre)
        .setLastModifiedAt(observedAt);
  }

  private MasterStyleRecord getMasterStyleRecord(
      Integer masterId, String style, int ordinal, LocalDateTime observedAt) {
    return new MasterStyleRecord()
        .setMasterId(masterId)
        .setOrdinal(ordinal)
        .setStyle(style)
        .setLastModifiedAt(observedAt);
  }

  private MasterVideoRecord getMasterVideoRecord(
      Integer masterId,
      MasterSubItemsXML.MasterVideoXML video,
      int ordinal,
      LocalDateTime observedAt) {
    String hashSrc =
        (video.getTitle() == null ? "" : video.getTitle())
            + (video.getDescription() == null ? "" : video.getDescription())
            + video.getUrl();
    return new MasterVideoRecord()
        .setMasterId(masterId)
        .setOrdinal(ordinal)
        .setTitle(video.getTitle())
        .setDescription(video.getDescription())
        .setUrl(video.getUrl())
        .setHash(hashSrc.hashCode())
        .setLastModifiedAt(observedAt);
  }
}
