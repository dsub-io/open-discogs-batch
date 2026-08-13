package io.dsub.discogs.batch.job.processor;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.ARTIST;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.ArtistAliasRecord;
import io.dsub.opendiscogs.jooq.tables.records.ArtistGroupRecord;
import io.dsub.opendiscogs.jooq.tables.records.ArtistMemberRecord;
import io.dsub.opendiscogs.jooq.tables.records.ArtistNameVariationRecord;
import io.dsub.opendiscogs.jooq.tables.records.ArtistUrlRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jooq.UpdatableRecord;

public class ArtistSubItemsProcessor
    implements ObservedAtItemProcessor<ArtistSubItemsXML, RelationSet> {

  private final EntityIdRegistry idRegistry;

  public ArtistSubItemsProcessor(EntityIdRegistry idRegistry) {
    this.idRegistry = Objects.requireNonNull(idRegistry, "idRegistry must not be null");
  }

  @Override
  public RelationSet process(ArtistSubItemsXML item, LocalDateTime observedAt) {

    if (item.getId() == null || item.getId() < 1) {
      return null;
    }

    ReflectionUtil.normalizeStringFields(item);

    List<UpdatableRecord<?>> items = new ArrayList<>();

    items.addAll(getArtistAliasRecords(item, observedAt));
    items.addAll(getArtistGroupRecords(item, observedAt));
    items.addAll(getArtistMemberRecords(item, observedAt));
    items.addAll(getArtistUrlRecords(item, observedAt));
    items.addAll(getArtistNameVariationRecords(item, observedAt));

    return new RelationSet(EntityType.ARTIST, item.getId(), items);
  }

  private List<ArtistNameVariationRecord> getArtistNameVariationRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getNameVariations() == null || item.getNameVariations().isEmpty()) {
      return Collections.emptyList();
    }
    return item.getNameVariations().stream()
        .filter(Objects::nonNull)
        .map(nameVar -> makeArtistNameVariationRecord(item.getId(), nameVar, observedAt))
        .collect(Collectors.toList());
  }

  private List<ArtistUrlRecord> getArtistUrlRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getUrls() == null || item.getUrls().isEmpty()) {
      return Collections.emptyList();
    }
    return item.getUrls().stream()
        .filter(Objects::nonNull)
        .map(url -> makeArtistUrlRecord(item.getId(), url, observedAt))
        .collect(Collectors.toList());
  }

  private List<ArtistMemberRecord> getArtistMemberRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getMembers() == null || item.getMembers().isEmpty()) {
      return Collections.emptyList();
    }
    return item.getMembers().stream()
        .filter(Objects::nonNull)
        .filter(member -> idRegistry.exists(ARTIST, member.getMemberId()))
        .map(xml -> xml.getRecord(item.getId(), observedAt))
        .collect(Collectors.toList());
  }

  private List<ArtistGroupRecord> getArtistGroupRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getGroups() == null || item.getGroups().isEmpty()) {
      return Collections.emptyList();
    }
    return item.getGroups().stream()
        .filter(Objects::nonNull)
        .filter(group -> idRegistry.exists(ARTIST, group.getGroupId()))
        .map(xml -> xml.getRecord(item.getId(), observedAt))
        .collect(Collectors.toList());
  }

  private List<ArtistAliasRecord> getArtistAliasRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getAliases() == null || item.getAliases().isEmpty()) {
      return Collections.emptyList();
    }
    return item.getAliases().stream()
        .filter(Objects::nonNull)
        .filter(alias -> idRegistry.exists(ARTIST, alias.getAliasId()))
        .map(xml -> xml.getRecord(item.getId(), observedAt))
        .collect(Collectors.toList());
  }

  private ArtistNameVariationRecord makeArtistNameVariationRecord(
      Integer artistId, String nameVar, LocalDateTime observedAt) {
    ArtistNameVariationRecord record = new ArtistNameVariationRecord();
    return record
        .setArtistId(artistId)
        .setNameVariation(nameVar)
        .setHash(nameVar.hashCode())
        .setLastModifiedAt(observedAt)
        .setCreatedAt(observedAt);
  }

  private ArtistUrlRecord makeArtistUrlRecord(
      Integer artistId, String url, LocalDateTime observedAt) {
    ArtistUrlRecord record = new ArtistUrlRecord();
    return record
        .setUrl(url)
        .setArtistId(artistId)
        .setCreatedAt(observedAt)
        .setHash(url.hashCode())
        .setLastModifiedAt(observedAt);
  }
}
