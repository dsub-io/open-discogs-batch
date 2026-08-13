package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemStyleRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jooq.TableRecord;

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
    ReflectionUtil.normalizeReleaseStringFields(item);
    return processNormalized(item, observedAt);
  }

  RelationSet processNormalized(ReleaseItemSubItemsXML item, LocalDateTime observedAt) {
    if (item.getId() == null || item.getId() < 1) {
      return null;
    }
    List<TableRecord<?>> items = new ArrayList<>();
    int releaseItemId = item.getId();

    if (item.getReleaseAlbumArtists() != null) {
      item.getReleaseAlbumArtists().stream()
          .filter(Objects::nonNull)
          .filter(albumArtist -> isExistingArtist(albumArtist.getArtistId()))
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
    }
    if (item.getCompanies() != null) {
      item.getCompanies().stream()
          .filter(Objects::nonNull)
          .filter(work -> isExistingLabel(work.getId()))
          .filter(work -> hasText(work.getWork()))
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
    }
    if (item.getReleaseCreditedArtists() != null) {
      item.getReleaseCreditedArtists().stream()
          .filter(Objects::nonNull)
          .filter(creditedArtist -> isExistingArtist(creditedArtist.getArtistId()))
          .filter(creditedArtist -> hasText(creditedArtist.getRole()))
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
    }
    if (item.getReleaseFormats() != null) {
      item.getReleaseFormats().stream()
          .filter(Objects::nonNull)
          .filter(this::hasFormatIdentity)
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
    }

    if (item.getGenres() != null) {
      item.getGenres().stream()
          .filter(Objects::nonNull)
          .filter(this::hasText)
          .map(
              genre ->
                  new ReleaseItemGenreRecord()
                      .setReleaseItemId(releaseItemId)
                      .setGenre(genre)
                      .setLastModifiedAt(observedAt))
          .forEach(items::add);
    }

    if (item.getStyles() != null) {
      item.getStyles().stream()
          .filter(Objects::nonNull)
          .filter(this::hasText)
          .map(
              style ->
                  new ReleaseItemStyleRecord()
                      .setReleaseItemId(releaseItemId)
                      .setStyle(style)
                      .setLastModifiedAt(observedAt))
          .forEach(items::add);
    }

    if (item.getReleaseIdentifiers() != null) {
      item.getReleaseIdentifiers().stream()
          .filter(Objects::nonNull)
          .filter(
              identifier ->
                  hasText(
                      identifier.getType(),
                      identifier.getDescription(),
                      identifier.getValue()))
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
    }

    if (item.getLabelReleaseLabels() != null) {
      item.getLabelReleaseLabels().stream()
          .filter(Objects::nonNull)
          .filter(label -> isExistingLabel(label.getLabelId()))
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
    }

    if (item.getReleaseTracks() != null) {
      item.getReleaseTracks().stream()
          .filter(Objects::nonNull)
          .filter(
              track -> hasText(track.getPosition(), track.getTitle(), track.getDuration()))
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
    }

    if (item.getReleaseVideos() != null) {
      item.getReleaseVideos().stream()
          .filter(Objects::nonNull)
          .filter(video -> hasText(video.getTitle(), video.getDescription(), video.getUrl()))
          .map(xml -> xml.getRecord(releaseItemId, observedAt))
          .forEach(items::add);
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
