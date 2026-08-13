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

    List<String> sourceUrls = item.getUrls();
    List<String> sourceNameVariations = item.getNameVariations();
    ReflectionUtil.normalizeStringFields(item);

    List<UpdatableRecord<?>> items = new ArrayList<>();

    items.addAll(getArtistAliasRecords(item, observedAt));
    items.addAll(getArtistGroupRecords(item, observedAt));
    items.addAll(getArtistMemberRecords(item, observedAt));
    items.addAll(getArtistUrlRecords(item.getId(), sourceUrls, observedAt));
    items.addAll(
        getArtistNameVariationRecords(item.getId(), sourceNameVariations, observedAt));

    return new RelationSet(EntityType.ARTIST, item.getId(), items);
  }

  private List<ArtistNameVariationRecord> getArtistNameVariationRecords(
      Integer artistId, List<String> sourceValues, LocalDateTime observedAt) {
    if (sourceValues == null || sourceValues.isEmpty()) {
      return Collections.emptyList();
    }
    List<ArtistNameVariationRecord> records = new ArrayList<>();
    for (int ordinal = 0; ordinal < sourceValues.size(); ordinal++) {
      String nameVariation = normalizeLegacyString(sourceValues.get(ordinal));
      if (nameVariation != null) {
        records.add(
            makeArtistNameVariationRecord(artistId, nameVariation, ordinal, observedAt));
      }
    }
    return records;
  }

  private List<ArtistUrlRecord> getArtistUrlRecords(
      Integer artistId, List<String> sourceValues, LocalDateTime observedAt) {
    if (sourceValues == null || sourceValues.isEmpty()) {
      return Collections.emptyList();
    }
    List<ArtistUrlRecord> records = new ArrayList<>();
    for (int ordinal = 0; ordinal < sourceValues.size(); ordinal++) {
      String url = normalizeLegacyString(sourceValues.get(ordinal));
      if (url != null) {
        records.add(makeArtistUrlRecord(artistId, url, ordinal, observedAt));
      }
    }
    return records;
  }

  private List<ArtistMemberRecord> getArtistMemberRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getMembers() == null || item.getMembers().isEmpty()) {
      return Collections.emptyList();
    }
    List<ArtistMemberRecord> records = new ArrayList<>();
    for (int ordinal = 0; ordinal < item.getMembers().size(); ordinal++) {
      ArtistSubItemsXML.ArtistMemberXML member = item.getMembers().get(ordinal);
      if (member != null && idRegistry.exists(ARTIST, member.getMemberId())) {
        records.add(member.getRecord(item.getId(), observedAt).setOrdinal(ordinal));
      }
    }
    return records;
  }

  private List<ArtistGroupRecord> getArtistGroupRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getGroups() == null || item.getGroups().isEmpty()) {
      return Collections.emptyList();
    }
    List<ArtistGroupRecord> records = new ArrayList<>();
    for (int ordinal = 0; ordinal < item.getGroups().size(); ordinal++) {
      ArtistSubItemsXML.ArtistGroupXML group = item.getGroups().get(ordinal);
      if (group != null && idRegistry.exists(ARTIST, group.getGroupId())) {
        records.add(group.getRecord(item.getId(), observedAt).setOrdinal(ordinal));
      }
    }
    return records;
  }

  private List<ArtistAliasRecord> getArtistAliasRecords(
      ArtistSubItemsXML item, LocalDateTime observedAt) {
    if (item.getAliases() == null || item.getAliases().isEmpty()) {
      return Collections.emptyList();
    }
    List<ArtistAliasRecord> records = new ArrayList<>();
    for (int ordinal = 0; ordinal < item.getAliases().size(); ordinal++) {
      ArtistSubItemsXML.ArtistAliasXML alias = item.getAliases().get(ordinal);
      if (alias != null && idRegistry.exists(ARTIST, alias.getAliasId())) {
        records.add(alias.getRecord(item.getId(), observedAt).setOrdinal(ordinal));
      }
    }
    return records;
  }

  private ArtistNameVariationRecord makeArtistNameVariationRecord(
      Integer artistId, String nameVar, int ordinal, LocalDateTime observedAt) {
    ArtistNameVariationRecord record = new ArtistNameVariationRecord();
    return record
        .setArtistId(artistId)
        .setOrdinal(ordinal)
        .setNameVariation(nameVar)
        .setHash(nameVar.hashCode())
        .setLastModifiedAt(observedAt);
  }

  private ArtistUrlRecord makeArtistUrlRecord(
      Integer artistId, String url, int ordinal, LocalDateTime observedAt) {
    ArtistUrlRecord record = new ArtistUrlRecord();
    return record
        .setUrl(url)
        .setArtistId(artistId)
        .setOrdinal(ordinal)
        .setHash(url.hashCode())
        .setLastModifiedAt(observedAt);
  }

  private String normalizeLegacyString(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isBlank() ? null : normalized;
  }
}
