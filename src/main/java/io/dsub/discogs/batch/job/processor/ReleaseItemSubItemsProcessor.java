package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemStyleRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jooq.UpdatableRecord;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class ReleaseItemSubItemsProcessor
    implements ItemProcessor<ReleaseItemSubItemsXML, Collection<UpdatableRecord<?>>> {

  private final EntityIdRegistry idRegistry;

  @Override
  public Collection<UpdatableRecord<?>> process(ReleaseItemSubItemsXML item) {
    if (item.getId() == null || item.getId() < 1) {
      return null;
    }
    ReflectionUtil.normalizeStringFields(item);
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
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }
    if (item.getReleaseCreditedArtists() != null) {
      item.getReleaseCreditedArtists().stream()
          .filter(Objects::nonNull)
          .filter(creditedArtist -> isExistingArtist(creditedArtist.getArtistId()))
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }
    if (item.getReleaseFormats() != null) {
      item.getReleaseFormats().stream()
          .filter(Objects::nonNull)
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }

    if (item.getGenres() != null) {
      item.getGenres().stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(this::isExistingGenre)
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
          .map(String::trim)
          .filter(this::isExistingStyle)
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
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }

    if (item.getReleaseVideos() != null) {
      item.getReleaseVideos().stream()
          .filter(Objects::nonNull)
          .filter(vid -> vid.getUrl() != null && !vid.getUrl().isBlank())
          .distinct()
          .map(xml -> xml.getRecord(releaseItemId))
          .forEach(items::add);
    }

    return items.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  private boolean isExistingArtist(Integer id) {
    if (id == null || id < 1) {
      return false;
    }
    return idRegistry.exists(EntityIdRegistry.Type.ARTIST, id);
  }

  private boolean isExistingLabel(Integer id) {
    if (id == null || id < 1) {
      return false;
    }
    return idRegistry.exists(EntityIdRegistry.Type.LABEL, id);
  }

  private boolean isExistingGenre(String genre) {
    if (genre == null || genre.isBlank()) {
      return false;
    }
    return idRegistry.exists(EntityIdRegistry.Type.GENRE, genre);
  }

  private boolean isExistingStyle(String style) {
    if (style == null || style.isBlank()) {
      return false;
    }
    return idRegistry.exists(EntityIdRegistry.Type.STYLE, style);
  }
}
