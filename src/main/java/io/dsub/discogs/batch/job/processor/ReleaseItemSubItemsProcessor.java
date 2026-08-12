package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.batch.util.DiscogsStringNormalizer;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemStyleRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jooq.UpdatableRecord;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@RequiredArgsConstructor
public class ReleaseItemSubItemsProcessor
    implements ItemProcessor<ReleaseItemSubItemsXML, RelationSet> {

  private final EntityIdRegistry idRegistry;

  @Override
  public RelationSet process(ReleaseItemSubItemsXML item) {
    if (item.getId() == null || item.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeReleaseStringFields(item);
    List<UpdatableRecord<?>> items = new ArrayList<>();
    int releaseItemId = item.getId();

    if (item.getReleaseAlbumArtists() != null) {
      item.getReleaseAlbumArtists().stream()
          .filter(Objects::nonNull)
          .filter(albumArtist -> isExistingArtist(albumArtist.getArtistId()))
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }
    if (item.getCompanies() != null) {
      item.getCompanies().stream()
          .filter(Objects::nonNull)
          .filter(work -> isExistingLabel(work.getId()))
          .filter(work -> hasText(work.getWork()))
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }
    if (item.getReleaseCreditedArtists() != null) {
      item.getReleaseCreditedArtists().stream()
          .filter(Objects::nonNull)
          .filter(creditedArtist -> isExistingArtist(creditedArtist.getArtistId()))
          .filter(creditedArtist -> hasText(creditedArtist.getRole()))
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }
    if (item.getReleaseFormats() != null) {
      item.getReleaseFormats().stream()
          .filter(Objects::nonNull)
          .filter(this::hasFormatIdentity)
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }

    if (item.getGenres() != null) {
      item.getGenres().stream()
          .filter(Objects::nonNull)
          .map(DiscogsStringNormalizer::normalizeNullable)
          .filter(Objects::nonNull)
          .filter(this::hasText)
          .distinct()
          .map(
              genre ->
                  new ReleaseItemGenreRecord()
                      .setReleaseItemId(releaseItemId)
                      .setGenre(genre)
                      .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
                      .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC())))
          .forEach(items::add);
    }

    if (item.getStyles() != null) {
      item.getStyles().stream()
          .filter(Objects::nonNull)
          .map(DiscogsStringNormalizer::normalizeNullable)
          .filter(Objects::nonNull)
          .filter(this::hasText)
          .distinct()
          .map(
              style ->
                  new ReleaseItemStyleRecord()
                      .setReleaseItemId(releaseItemId)
                      .setStyle(style)
                      .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
                      .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC())))
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
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }

    if (item.getLabelReleaseLabels() != null) {
      item.getLabelReleaseLabels().stream()
          .filter(Objects::nonNull)
          .filter(label -> isExistingLabel(label.getLabelId()))
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }

    if (item.getReleaseTracks() != null) {
      item.getReleaseTracks().stream()
          .filter(Objects::nonNull)
          .filter(
              track -> hasText(track.getPosition(), track.getTitle(), track.getDuration()))
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }

    if (item.getReleaseVideos() != null) {
      item.getReleaseVideos().stream()
          .filter(Objects::nonNull)
          .filter(video -> hasText(video.getTitle(), video.getDescription(), video.getUrl()))
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
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
