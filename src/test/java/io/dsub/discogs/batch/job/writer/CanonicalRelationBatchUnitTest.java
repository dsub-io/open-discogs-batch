package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.domain.release.ReleaseRelationIdentity;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.LabelReleaseItem;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemFormat;
import io.dsub.opendiscogs.jooq.tables.records.ArtistUrlRecord;
import io.dsub.opendiscogs.jooq.tables.records.LabelReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemCreditedArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemFormatRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemIdentifierRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemStyleRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemTrackRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemVideoRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemWorkRecord;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CanonicalRelationBatchUnitTest {

  private static final int RELEASE_ID = 2;

  @Test
  void collapsesExactDuplicatesByConflictTargetAndPreservesCatalogSpellings() {
    List<UpdatableRecord<?>> records =
        List.of(
            artist(),
            artist(),
            label(null),
            label(null),
            label("SK 026"),
            label("SK026"),
            genre(),
            genre(),
            style(),
            style(),
            format(1, "Vinyl"),
            format(1, "Vinyl"),
            track("Track"),
            track("Track"),
            identifier("123"),
            identifier("123"),
            work("Pressed By"),
            work("Pressed By"),
            video("Video"),
            video("Video"),
            creditedArtist("Producer"),
            creditedArtist("Producer"));

    CanonicalRelationBatch.CanonicalBatch batch =
        CanonicalRelationBatch.prepare(
            List.of(new RelationSet(EntityType.RELEASE, RELEASE_ID, records)),
            EntityType.RELEASE);
    List<RelationSet> result = batch.relationSets();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().records()).hasSize(12);
    assertThat(result.getFirst().records())
        .filteredOn(record -> record instanceof LabelReleaseItemRecord)
        .extracting(record -> record.get("category_notation"))
        .containsExactly(null, "SK 026", "SK026");
    RelationTableRegistry.RelationTable labels =
        RelationTableRegistry.require(EntityType.RELEASE, LabelReleaseItem.LABEL_RELEASE_ITEM);
    assertThat(batch.recordsFor(labels)).hasSize(3);
  }

  @Test
  void allocatesDistinctSlotsForTheKnownDiscogsTrackCollision() {
    ReleaseItemTrackRecord first =
        track("Яд").setPosition("6").setDuration(null).setHash(86_171);
    first.setIdentitySha256(
        ReleaseRelationIdentity.digest(
            ReleaseRelationIdentity.Relation.TRACK, "6", "Яд", null));
    ReleaseItemTrackRecord second =
        track("Ад").setPosition("7").setDuration(null).setHash(86_171);
    second.setIdentitySha256(
        ReleaseRelationIdentity.digest(
            ReleaseRelationIdentity.Relation.TRACK, "7", "Ад", null));

    List<RelationSet> result =
        CanonicalRelationBatch.canonicalize(
            List.of(new RelationSet(EntityType.RELEASE, RELEASE_ID, List.of(first, second))),
            EntityType.RELEASE);

    assertThat(result.getFirst().records()).hasSize(2);
    assertThat(first.getHash()).isEqualTo(86_171);
    assertThat(second.getHash()).isEqualTo(-947_370_883);
  }

  @Test
  void collapsesOneSemanticIdentityBeforeConsideringDifferentLegacyHashes() {
    ReleaseItemTrackRecord first = track("Track").setHash(86_171);
    ReleaseItemTrackRecord second = track("Track").setHash(86_172);

    List<RelationSet> canonical =
        CanonicalRelationBatch.canonicalize(
            List.of(
                new RelationSet(
                    EntityType.RELEASE, RELEASE_ID, List.of(first, second))),
            EntityType.RELEASE);

    assertThat(canonical.getFirst().records()).containsExactly(first);
    assertThat(first.getHash()).isEqualTo(86_171);
  }

  @Test
  void rejectsDifferentPayloadForOneCanonicalIdentity() {
    ReleaseItemFormatRecord first = format(1, "Vinyl");
    ReleaseItemFormatRecord second = format(1, "Vinyl").setHash(102).setQuantity(2);

    assertThatThrownBy(
            () ->
                CanonicalRelationBatch.canonicalize(
                    List.of(
                        new RelationSet(
                            EntityType.RELEASE, RELEASE_ID, List.of(first, second))),
                    EntityType.RELEASE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("conflicting persisted payload")
        .hasMessageContaining("identity_sha256");
  }

  @Test
  void derivesFormatDigestFromCompatibilityQuantityWhenCanonicalTextIsAbsent() {
    ReleaseItemFormatRecord record = format(2, "Vinyl").setQuantityText(null);

    ReleaseRelationSlotAllocator.assignCanonicalDigests(
        List.of(
            new RelationSet(EntityType.RELEASE, RELEASE_ID, List.of(record))),
        EntityType.RELEASE);

    assertThat(record.getIdentitySha256())
        .containsExactly(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.FORMAT,
                "Vinyl",
                "LP",
                "2",
                "Limited"));
  }

  @Test
  void validatesAssignedReleaseDigests() {
    ReleaseItemFormatRecord absentQuantity =
        format(1, "Vinyl").setQuantity(null).setQuantityText(null);
    ReleaseRelationSlotAllocator.assignCanonicalDigests(
        List.of(new RelationSet(EntityType.RELEASE, RELEASE_ID, List.of(absentQuantity))),
        EntityType.RELEASE);
    assertThat(absentQuantity.getIdentitySha256()).hasSize(32);

    ReleaseItemFormatRecord missingDigest = format(1, "Vinyl").setIdentitySha256(null);
    ReleaseItemFormatRecord missingHash = format(1, "Vinyl").setHash(null);
    ReleaseItemFormatRecord shortDigest = format(1, "Vinyl").setIdentitySha256(new byte[] {1});
    for (ReleaseItemFormatRecord invalid : List.of(missingDigest, missingHash, shortDigest)) {
      assertThatThrownBy(
              () ->
                  ReleaseRelationSlotAllocator.allocateAssignedDigests(
                      List.of(
                          new RelationSet(EntityType.RELEASE, RELEASE_ID, List.of(invalid))),
                      EntityType.RELEASE))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("identity is incomplete");
    }

    ReleaseItemFormatRecord duplicateFirst = format(1, "Vinyl");
    ReleaseItemFormatRecord duplicateSecond = format(1, "Vinyl");
    assertThatThrownBy(
            () ->
                ReleaseRelationSlotAllocator.allocateAssignedDigests(
                    List.of(
                        new RelationSet(
                            EntityType.RELEASE,
                            RELEASE_ID,
                            List.of(duplicateFirst, duplicateSecond))),
                    EntityType.RELEASE))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be canonicalized");
  }

  @Test
  void slotAllocationSkipsReservedAndAssignedCandidatesAndDetectsExhaustion() {
    ReleaseItemTrackRecord first = track("first").setHash(102);
    ReleaseItemTrackRecord second = track("second").setHash(102);
    ReleaseItemTrackRecord third = track("third").setHash(102);
    List<RelationSet> relationSets =
        List.of(
            new RelationSet(EntityType.RELEASE, RELEASE_ID, List.of(first, second, third)));

    ReleaseRelationSlotAllocator.allocate(
        relationSets,
        EntityType.RELEASE,
        3,
        (relation, digest, attempt) ->
            switch (attempt) {
              case 0 -> 102;
              case 1 -> 200;
              default -> 201;
            });
    assertThat(List.of(first.getHash(), second.getHash(), third.getHash()))
        .containsExactlyInAnyOrder(102, 200, 201);

    ReleaseItemTrackRecord exhaustedFirst = track("first").setHash(102);
    ReleaseItemTrackRecord exhaustedSecond = track("second").setHash(102);
    assertThatThrownBy(
            () ->
                ReleaseRelationSlotAllocator.allocate(
                    List.of(
                        new RelationSet(
                            EntityType.RELEASE,
                            RELEASE_ID,
                            List.of(exhaustedFirst, exhaustedSecond))),
                    EntityType.RELEASE,
                    1,
                    (relation, digest, attempt) -> 102))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("slot space exhausted");
  }

  @Test
  void digestKeyUsesByteContentEquality() {
    ReleaseRelationSlotAllocator.DigestKey first =
        new ReleaseRelationSlotAllocator.DigestKey(new byte[] {1});
    ReleaseRelationSlotAllocator.DigestKey same =
        new ReleaseRelationSlotAllocator.DigestKey(new byte[] {1});
    ReleaseRelationSlotAllocator.DigestKey different =
        new ReleaseRelationSlotAllocator.DigestKey(new byte[] {2});

    assertThat(first).isEqualTo(same).isNotEqualTo(different).isNotEqualTo("not a digest");
  }

  @Test
  void rejectsUnsupportedTypedIdentityShapes() {
    ReleaseItemFormatRecord record = format(1, "Vinyl");
    RelationTableRegistry.RelationKey integer =
        new RelationTableRegistry.RelationKey(
            ReleaseItemFormat.RELEASE_ITEM_FORMAT.RELEASE_ITEM_ID,
            RelationTableRegistry.RelationKeyType.INTEGER);
    RelationTableRegistry.RelationKey binary =
        new RelationTableRegistry.RelationKey(
            ReleaseItemFormat.RELEASE_ITEM_FORMAT.IDENTITY_SHA256,
            RelationTableRegistry.RelationKeyType.BINARY);

    assertThatThrownBy(
            () ->
                RelationTableRegistry.RelationIdentity.create(
                    ReleaseItemFormat.RELEASE_ITEM_FORMAT, List.of(integer), record))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unsupported canonical relation key shape");
    assertThat(
            RelationTableRegistry.RelationIdentity.create(
                ReleaseItemFormat.RELEASE_ITEM_FORMAT, List.of(integer, binary), record))
        .isInstanceOf(RelationTableRegistry.IntegerBinaryIdentity.class);
    assertThatThrownBy(
            () ->
                RelationTableRegistry.RelationIdentity.create(
                    ReleaseItemFormat.RELEASE_ITEM_FORMAT,
                    List.of(integer, integer, integer),
                    record))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                RelationTableRegistry.RelationIdentity.create(
                    ReleaseItemFormat.RELEASE_ITEM_FORMAT,
                    List.of(binary, integer, integer, binary),
                    record))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validatesRelationOwnershipAndRegisteredTable() throws Exception {
    assertThatThrownBy(
            () ->
                CanonicalRelationBatch.canonicalize(
                    List.of(
                        new RelationSet(
                            EntityType.RELEASE,
                            RELEASE_ID,
                            List.of(new ReleaseItemArtistRecord().setReleaseItemId(3)
                                .setArtistId(5)))),
                    EntityType.RELEASE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not belong to root");

    assertThatThrownBy(
            () ->
                CanonicalRelationBatch.canonicalize(
                    List.of(
                        new RelationSet(
                            EntityType.RELEASE,
                            RELEASE_ID,
                            List.of(new ArtistUrlRecord()))),
                    EntityType.RELEASE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not a release relation");

    PreparedStatement statement = mock(PreparedStatement.class);
    RelationTableRegistry.RelationTable formatTable =
        RelationTableRegistry.require(EntityType.RELEASE, ReleaseItemFormat.RELEASE_ITEM_FORMAT);
    RelationTableRegistry.RelationKey identityKey =
        formatTable.keys().stream()
            .filter(key -> key.field().getName().equals("identity_sha256"))
            .findFirst()
            .orElseThrow();
    ReleaseItemFormatRecord format = format(1, "Vinyl").setIdentitySha256(null);
    identityKey.bind(statement, 1, format);
    verify(statement).setNull(1, Types.BINARY);
    format.setIdentitySha256(new byte[] {1});
    identityKey.bind(statement, 2, format);
    ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
    verify(statement).setBytes(org.mockito.ArgumentMatchers.eq(2), bytes.capture());
    assertThat(bytes.getValue()).containsExactly(1);
    RelationTableRegistry.RelationKey integerKey = formatTable.keys().getFirst();
    integerKey.bind(statement, 3, format);
    verify(statement).setInt(3, RELEASE_ID);
    RelationTableRegistry.RelationTable labelTable =
        RelationTableRegistry.require(EntityType.RELEASE, LabelReleaseItem.LABEL_RELEASE_ITEM);
    labelTable.keys().getLast().bind(statement, 4, label(null));
    verify(statement).setNull(4, Types.VARCHAR);
    ReleaseItemFormatRecord missingRelease = format(1, "Vinyl").setReleaseItemId(null);
    integerKey.bind(statement, 5, missingRelease);
    verify(statement).setNull(5, Types.INTEGER);
    labelTable.keys().getLast().bind(statement, 6, label("SK 026"));
    verify(statement).setString(6, "SK 026");
    assertThatThrownBy(
            () ->
                new RelationTableRegistry.RelationKey(
                    ReleaseItemFormat.RELEASE_ITEM_FORMAT.RELEASE_ITEM_ID, null))
        .isInstanceOf(NullPointerException.class);

    assertThat(new RelationTableRegistry.BinaryKey(null).value()).isNull();
    byte[] source = new byte[] {1};
    RelationTableRegistry.BinaryKey binary = new RelationTableRegistry.BinaryKey(source);
    source[0] = 2;
    byte[] copy = binary.value();
    copy[0] = 3;
    assertThat(binary.value()).containsExactly(1);
    RelationTableRegistry.BinaryKey same = new RelationTableRegistry.BinaryKey(new byte[] {1});
    assertThat(binary)
        .isEqualTo(same)
        .isNotEqualTo(new RelationTableRegistry.BinaryKey(new byte[] {2}))
        .isNotEqualTo("not binary");
    assertThat(binary.hashCode()).isEqualTo(same.hashCode());
  }

  private ReleaseItemArtistRecord artist() {
    return new ReleaseItemArtistRecord().setReleaseItemId(RELEASE_ID).setArtistId(5);
  }

  private LabelReleaseItemRecord label(String categoryNotation) {
    return new LabelReleaseItemRecord()
        .setReleaseItemId(RELEASE_ID)
        .setLabelId(5)
        .setCategoryNotation(categoryNotation);
  }

  private ReleaseItemGenreRecord genre() {
    return new ReleaseItemGenreRecord().setReleaseItemId(RELEASE_ID).setGenre("Rock");
  }

  private ReleaseItemStyleRecord style() {
    return new ReleaseItemStyleRecord().setReleaseItemId(RELEASE_ID).setStyle("House");
  }

  private ReleaseItemFormatRecord format(int quantity, String name) {
    return new ReleaseItemFormatRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(101)
        .setName(name)
        .setDescription("LP")
        .setText("Limited")
        .setQuantity(quantity)
        .setQuantityText(Integer.toString(quantity))
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.FORMAT,
                name,
                "LP",
                Integer.toString(quantity),
                "Limited"));
  }

  private ReleaseItemTrackRecord track(String title) {
    return new ReleaseItemTrackRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(102)
        .setPosition("A1")
        .setTitle(title)
        .setDuration("3:00")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "A1", title, "3:00"));
  }

  private ReleaseItemIdentifierRecord identifier(String value) {
    return new ReleaseItemIdentifierRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(103)
        .setType("Barcode")
        .setDescription("Text")
        .setValue(value)
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.IDENTIFIER, "Barcode", "Text", value));
  }

  private ReleaseItemWorkRecord work(String work) {
    return new ReleaseItemWorkRecord()
        .setReleaseItemId(RELEASE_ID)
        .setLabelId(5)
        .setHash(104)
        .setWork(work)
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(ReleaseRelationIdentity.Relation.WORK, work));
  }

  private ReleaseItemVideoRecord video(String title) {
    return new ReleaseItemVideoRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(105)
        .setTitle(title)
        .setDescription("Description")
        .setUrl("https://video.example")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.VIDEO,
                title,
                "Description",
                "https://video.example"));
  }

  private ReleaseItemCreditedArtistRecord creditedArtist(String role) {
    return new ReleaseItemCreditedArtistRecord()
        .setReleaseItemId(RELEASE_ID)
        .setArtistId(5)
        .setHash(106)
        .setRole(role)
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.CREDITED_ARTIST, role));
  }
}
