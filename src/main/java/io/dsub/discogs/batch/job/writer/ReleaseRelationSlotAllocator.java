package io.dsub.discogs.batch.job.writer;

import static io.dsub.opendiscogs.jooq.tables.ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST;
import static io.dsub.opendiscogs.jooq.tables.ReleaseItemFormat.RELEASE_ITEM_FORMAT;
import static io.dsub.opendiscogs.jooq.tables.ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER;
import static io.dsub.opendiscogs.jooq.tables.ReleaseItemTrack.RELEASE_ITEM_TRACK;
import static io.dsub.opendiscogs.jooq.tables.ReleaseItemVideo.RELEASE_ITEM_VIDEO;
import static io.dsub.opendiscogs.jooq.tables.ReleaseItemWork.RELEASE_ITEM_WORK;

import io.dsub.discogs.batch.domain.release.ReleaseRelationIdentity;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.Field;
import org.jooq.TableRecord;

/** Allocates deterministic legacy hash slots for distinct SHA-256 relation identities. */
final class ReleaseRelationSlotAllocator {

  private static final String HASH_FIELD = "hash";
  private static final String IDENTITY_FIELD = "identity_sha256";
  private static final long UNSIGNED_INT_COUNT = 1L << Integer.SIZE;

  private static final Map<String, RelationDescriptor> RELATIONS =
      Map.of(
          RELEASE_ITEM_CREDITED_ARTIST.getName(),
              new RelationDescriptor(
                  ReleaseRelationIdentity.Relation.CREDITED_ARTIST,
                  List.of(RELEASE_ITEM_CREDITED_ARTIST.ROLE)),
          RELEASE_ITEM_FORMAT.getName(),
              new RelationDescriptor(
                  ReleaseRelationIdentity.Relation.FORMAT,
                  record ->
                      new String[] {
                          RELEASE_ITEM_FORMAT.NAME.getValue(record),
                          RELEASE_ITEM_FORMAT.DESCRIPTION.getValue(record),
                          canonicalFormatQuantity(record),
                          RELEASE_ITEM_FORMAT.TEXT.getValue(record)
                      }),
          RELEASE_ITEM_IDENTIFIER.getName(),
              new RelationDescriptor(
                  ReleaseRelationIdentity.Relation.IDENTIFIER,
                  List.of(
                      RELEASE_ITEM_IDENTIFIER.TYPE,
                      RELEASE_ITEM_IDENTIFIER.DESCRIPTION,
                      RELEASE_ITEM_IDENTIFIER.VALUE)),
          RELEASE_ITEM_TRACK.getName(),
              new RelationDescriptor(
                  ReleaseRelationIdentity.Relation.TRACK,
                  List.of(
                      RELEASE_ITEM_TRACK.POSITION,
                      RELEASE_ITEM_TRACK.TITLE,
                      RELEASE_ITEM_TRACK.DURATION)),
          RELEASE_ITEM_VIDEO.getName(),
              new RelationDescriptor(
                  ReleaseRelationIdentity.Relation.VIDEO,
                  List.of(
                      RELEASE_ITEM_VIDEO.TITLE,
                      RELEASE_ITEM_VIDEO.DESCRIPTION,
                      RELEASE_ITEM_VIDEO.URL)),
          RELEASE_ITEM_WORK.getName(),
              new RelationDescriptor(
                  ReleaseRelationIdentity.Relation.WORK,
                  List.of(RELEASE_ITEM_WORK.WORK)));

  private ReleaseRelationSlotAllocator() {
  }

  static void allocate(
      List<? extends RelationSet> relationSets,
      EntityType entityType,
      long attemptCount,
      CompatibilitySlotGenerator slotGenerator) {
    assignCanonicalDigests(relationSets, entityType);
    allocateAssignedDigests(relationSets, entityType, attemptCount, slotGenerator);
  }

  static void assignCanonicalDigests(
      List<? extends RelationSet> relationSets, EntityType entityType) {
    if (entityType != EntityType.RELEASE) {
      return;
    }
    for (RelationSet relationSet : relationSets) {
      for (TableRecord<?> record : relationSet.records()) {
        RelationDescriptor descriptor = RELATIONS.get(record.getTable().getName());
        if (descriptor == null) {
          continue;
        }
        Field<Integer> hashField = record.getTable().field(HASH_FIELD, Integer.class);
        Field<byte[]> identityField = record.getTable().field(IDENTITY_FIELD, byte[].class);
        Integer legacyHash = hashField.getValue(record);
        if (legacyHash == null) {
          throw new IllegalArgumentException(
              "release relation identity is incomplete for " + record.getTable().getName());
        }
        byte[] digest = descriptor.digest(record);
        record.set(identityField, digest);
      }
    }
  }

  static void allocateAssignedDigests(
      List<? extends RelationSet> relationSets, EntityType entityType) {
    allocateAssignedDigests(
        relationSets,
        entityType,
        UNSIGNED_INT_COUNT,
        ReleaseRelationIdentity::compatibilitySlot);
  }

  private static void allocateAssignedDigests(
      List<? extends RelationSet> relationSets,
      EntityType entityType,
      long attemptCount,
      CompatibilitySlotGenerator slotGenerator) {
    if (entityType != EntityType.RELEASE) {
      return;
    }
    Map<Scope, ScopeRows> scopes = new LinkedHashMap<>();
    for (RelationSet relationSet : relationSets) {
      for (TableRecord<?> record : relationSet.records()) {
        RelationDescriptor descriptor = RELATIONS.get(record.getTable().getName());
        if (descriptor == null) {
          continue;
        }
        Field<Integer> hashField = record.getTable().field(HASH_FIELD, Integer.class);
        Field<byte[]> identityField = record.getTable().field(IDENTITY_FIELD, byte[].class);
        Integer legacyHash = hashField.getValue(record);
        byte[] digest = identityField.getValue(record);
        if (legacyHash == null || digest == null || digest.length != 32) {
          throw new IllegalArgumentException(
              "release relation identity is incomplete for " + record.getTable().getName());
        }
        RelationTableRegistry.RelationTable relationTable =
            RelationTableRegistry.require(entityType, record.getTable());
        List<Object> scopeValues =
            relationTable.conflictFields().stream()
                .filter(field -> !field.getName().equals(HASH_FIELD))
                .map(field -> field.getValue(record))
                .map(Object.class::cast)
                .toList();
        Scope scope = new Scope(record.getTable().getName(), scopeValues);
        ScopeRows scopeRows =
            scopes.computeIfAbsent(
                scope,
                ignored -> new ScopeRows(descriptor.relation(), attemptCount, slotGenerator));
        scopeRows.add(new Row(record, hashField, legacyHash, digest));
      }
    }
    scopes.values().forEach(ScopeRows::allocate);
  }

  private record Scope(String table, List<Object> values) {

    Scope {
      values = List.copyOf(values);
    }
  }

  private record RelationDescriptor(
      ReleaseRelationIdentity.Relation relation, IdentityFieldValues identityFieldValues) {

    RelationDescriptor(
        ReleaseRelationIdentity.Relation relation, List<Field<String>> identityFields) {
      this(
          relation,
          record ->
              identityFields.stream()
                  .map(field -> field.getValue(record))
                  .toArray(String[]::new));
    }

    byte[] digest(TableRecord<?> record) {
      return ReleaseRelationIdentity.digest(relation, identityFieldValues.values(record));
    }
  }

  @FunctionalInterface
  private interface IdentityFieldValues {

    String[] values(TableRecord<?> record);
  }

  private static String canonicalFormatQuantity(TableRecord<?> record) {
    String quantityText = RELEASE_ITEM_FORMAT.QUANTITY_TEXT.getValue(record);
    if (quantityText != null) {
      return quantityText;
    }
    Integer quantity = RELEASE_ITEM_FORMAT.QUANTITY.getValue(record);
    return quantity == null ? null : Integer.toString(quantity);
  }

  private static final class ScopeRows {

    private final ReleaseRelationIdentity.Relation relation;
    private final long attemptCount;
    private final CompatibilitySlotGenerator slotGenerator;
    private final Set<Integer> reserved = new HashSet<>();
    private final Map<Integer, Map<DigestKey, List<Row>>> groups = new HashMap<>();
    private final Set<DigestKey> semanticIdentities = new HashSet<>();

    private ScopeRows(
        ReleaseRelationIdentity.Relation relation,
        long attemptCount,
        CompatibilitySlotGenerator slotGenerator) {
      this.relation = relation;
      this.attemptCount = attemptCount;
      this.slotGenerator = slotGenerator;
    }

    private void add(Row row) {
      DigestKey digest = new DigestKey(row.digest());
      if (!semanticIdentities.add(digest)) {
        throw new IllegalStateException(
            "release relation semantic identities must be canonicalized before slot allocation");
      }
      reserved.add(row.legacyHash());
      groups.computeIfAbsent(row.legacyHash(), ignored -> new LinkedHashMap<>())
          .computeIfAbsent(digest, ignored -> new ArrayList<>())
          .add(row);
    }

    private void allocate() {
      List<Integer> legacyHashes = groups.keySet().stream().sorted().toList();
      Set<Integer> assigned = new HashSet<>();
      for (Integer legacyHash : legacyHashes) {
        List<Map.Entry<DigestKey, List<Row>>> digestGroups =
            new ArrayList<>(groups.get(legacyHash).entrySet());
        digestGroups.sort(Map.Entry.comparingByKey());
        assign(digestGroups.getFirst().getValue(), legacyHash);
        assigned.add(legacyHash);
        for (Map.Entry<DigestKey, List<Row>> collided : digestGroups.subList(1, digestGroups.size())) {
          boolean allocated = false;
          for (long attempt = 0; attempt < attemptCount; attempt++) {
            int candidate =
                slotGenerator.generate(relation, collided.getKey().bytes(), (int) attempt);
            if (reserved.contains(candidate) || assigned.contains(candidate)) {
              continue;
            }
            assign(collided.getValue(), candidate);
            assigned.add(candidate);
            allocated = true;
            break;
          }
          if (!allocated) {
            throw new IllegalStateException("signed 32-bit release relation slot space exhausted");
          }
        }
      }
    }

    private void assign(List<Row> rows, int hash) {
      rows.forEach(row -> row.record().set(row.hashField(), hash));
    }
  }

  private record Row(
      TableRecord<?> record,
      Field<Integer> hashField,
      int legacyHash,
      byte[] digest) {
  }

  record DigestKey(byte[] bytes) implements Comparable<DigestKey> {

    DigestKey {
      bytes = Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public byte[] bytes() {
      return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public int compareTo(DigestKey other) {
      return Arrays.compareUnsigned(bytes, other.bytes);
    }

    @Override
    public boolean equals(Object candidate) {
      return candidate instanceof DigestKey other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }

  @FunctionalInterface
  interface CompatibilitySlotGenerator {

    int generate(ReleaseRelationIdentity.Relation relation, byte[] digest, int attempt);
  }
}
