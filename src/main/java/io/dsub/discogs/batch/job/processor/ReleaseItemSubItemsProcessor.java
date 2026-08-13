package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.DiscogsStringNormalizer;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemStyleRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jooq.UpdatableRecord;

public class ReleaseItemSubItemsProcessor
    implements ObservedAtItemProcessor<ReleaseItemSubItemsXML, RelationSet> {

  private final EntityIdRegistry idRegistry;

  public ReleaseItemSubItemsProcessor(EntityIdRegistry idRegistry) {
    this.idRegistry = Objects.requireNonNull(idRegistry, "idRegistry must not be null");
  }

  @Override
  public RelationSet process(ReleaseItemSubItemsXML item, LocalDateTime observedAt) {
    if (item == null) {
      return null;
    }
    List<String> sourceGenres = item.getGenres();
    List<String> sourceStyles = item.getStyles();
    ReflectionUtil.normalizeReleaseStringFields(item);
    return processNormalized(item, sourceGenres, sourceStyles, observedAt);
  }

  private RelationSet processNormalized(
      ReleaseItemSubItemsXML item,
      List<String> sourceGenres,
      List<String> sourceStyles,
      LocalDateTime observedAt) {
    if (item.getId() == null || item.getId() < 1) {
      return null;
    }
    List<UpdatableRecord<?>> items = new ArrayList<>();
    int releaseItemId = item.getId();

    if (item.getReleaseAlbumArtists() != null) {
      for (int ordinal = 0; ordinal < item.getReleaseAlbumArtists().size(); ordinal++) {
        ReleaseItemSubItemsXML.ReleaseAlbumArtist artist =
            item.getReleaseAlbumArtists().get(ordinal);
        if (artist != null && isExistingArtist(artist.getArtistId())) {
          items.add(artist.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }
    if (item.getCompanies() != null) {
      for (int ordinal = 0; ordinal < item.getCompanies().size(); ordinal++) {
        ReleaseItemSubItemsXML.ReleaseWork work = item.getCompanies().get(ordinal);
        if (work != null && isExistingLabel(work.getId()) && hasText(work.getWork())) {
          items.add(work.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }
    if (item.getReleaseCreditedArtists() != null) {
      for (int ordinal = 0; ordinal < item.getReleaseCreditedArtists().size(); ordinal++) {
        ReleaseItemSubItemsXML.ReleaseCreditedArtist artist =
            item.getReleaseCreditedArtists().get(ordinal);
        if (artist != null
            && isExistingArtist(artist.getArtistId())
            && hasText(artist.getRole())) {
          items.add(artist.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }
    if (item.getReleaseFormats() != null) {
      for (int ordinal = 0; ordinal < item.getReleaseFormats().size(); ordinal++) {
        ReleaseItemSubItemsXML.ReleaseFormat format = item.getReleaseFormats().get(ordinal);
        if (format != null && hasFormatIdentity(format)) {
          items.add(format.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }

    if (sourceGenres != null) {
      for (int ordinal = 0; ordinal < sourceGenres.size(); ordinal++) {
        String genre = DiscogsStringNormalizer.normalizeNullable(sourceGenres.get(ordinal));
        if (genre != null) {
          items.add(
              new ReleaseItemGenreRecord()
                  .setReleaseItemId(releaseItemId)
                  .setGenre(genre)
                  .setOrdinal(ordinal)
                  .setCreatedAt(observedAt)
                  .setLastModifiedAt(observedAt));
        }
      }
    }

    if (sourceStyles != null) {
      for (int ordinal = 0; ordinal < sourceStyles.size(); ordinal++) {
        String style = DiscogsStringNormalizer.normalizeNullable(sourceStyles.get(ordinal));
        if (style != null) {
          items.add(
              new ReleaseItemStyleRecord()
                  .setReleaseItemId(releaseItemId)
                  .setStyle(style)
                  .setOrdinal(ordinal)
                  .setCreatedAt(observedAt)
                  .setLastModifiedAt(observedAt));
        }
      }
    }

    if (item.getReleaseIdentifiers() != null) {
      for (int ordinal = 0; ordinal < item.getReleaseIdentifiers().size(); ordinal++) {
        ReleaseItemSubItemsXML.ReleaseIdentifier identifier =
            item.getReleaseIdentifiers().get(ordinal);
        if (identifier != null
            && hasText(
                identifier.getType(), identifier.getDescription(), identifier.getValue())) {
          items.add(identifier.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }

    if (item.getLabelReleaseLabels() != null) {
      for (int ordinal = 0; ordinal < item.getLabelReleaseLabels().size(); ordinal++) {
        ReleaseItemSubItemsXML.LabelItemRelease label =
            item.getLabelReleaseLabels().get(ordinal);
        if (label != null && isExistingLabel(label.getLabelId())) {
          items.add(label.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }

    if (item.getReleaseTracks() != null) {
      for (int ordinal = 0; ordinal < item.getReleaseTracks().size(); ordinal++) {
        ReleaseItemSubItemsXML.ReleaseTrack track = item.getReleaseTracks().get(ordinal);
        if (track != null
            && hasText(track.getPosition(), track.getTitle(), track.getDuration())) {
          items.add(track.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }

    if (item.getReleaseVideos() != null) {
      for (int ordinal = 0; ordinal < item.getReleaseVideos().size(); ordinal++) {
        ReleaseItemSubItemsXML.ReleaseVideo video = item.getReleaseVideos().get(ordinal);
        if (video != null && hasText(video.getTitle(), video.getDescription(), video.getUrl())) {
          items.add(video.getRecord(releaseItemId, observedAt).setOrdinal(ordinal));
        }
      }
    }

    return new RelationSet(
        EntityType.RELEASE, releaseItemId, items);
  }

  private boolean isExistingArtist(Integer id) {
    if (id == null || id < 1) {
      return false;
    }
    return idRegistry.exists(DefaultEntityIdRegistry.Type.ARTIST, id);
  }

  private boolean isExistingLabel(Integer id) {
    if (id == null || id < 1) {
      return false;
    }
    return idRegistry.exists(DefaultEntityIdRegistry.Type.LABEL, id);
  }

  private boolean hasFormatIdentity(ReleaseItemSubItemsXML.ReleaseFormat format) {
    return hasText(format.getName(), format.getText())
        || !Objects.requireNonNullElse(format.getDescriptions(), List.of()).isEmpty();
  }

  private boolean hasText(String... values) {
    return Arrays.stream(values).anyMatch(Objects::nonNull);
  }
}
