package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
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
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

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

    List<RelationSet> result =
        CanonicalRelationBatch.canonicalize(
            List.of(new RelationSet(EntityType.RELEASE, RELEASE_ID, records)),
            EntityType.RELEASE);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().records()).hasSize(12);
    assertThat(result.getFirst().records())
        .filteredOn(record -> record instanceof LabelReleaseItemRecord)
        .extracting(record -> record.get("category_notation"))
        .containsExactly(null, "SK 026", "SK026");
  }

  @TestFactory
  Stream<DynamicTest> rejectsHashCollisionsWithDifferentPersistedPayload() {
    return Stream.of(
            conflict("release_item_format", () -> format(1, "Vinyl"), () -> format(2, "Vinyl")),
            conflict("release_item_track", () -> track("First"), () -> track("Second")),
            conflict(
                "release_item_identifier",
                () -> identifier("111"),
                () -> identifier("222")),
            conflict("release_item_work", () -> work("Pressed By"), () -> work("Made By")),
            conflict("release_item_video", () -> video("First"), () -> video("Second")),
            conflict(
                "release_item_credited_artist",
                () -> creditedArtist("Producer"),
                () -> creditedArtist("Engineer")))
        .map(
            conflict ->
                DynamicTest.dynamicTest(
                    conflict.tableName(),
                    () ->
                        assertThatThrownBy(
                                () ->
                                    CanonicalRelationBatch.canonicalize(
                                        List.of(
                                            new RelationSet(
                                                EntityType.RELEASE,
                                                RELEASE_ID,
                                                List.of(
                                                    conflict.first().get(),
                                                    conflict.second().get()))),
                                        EntityType.RELEASE))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("conflicting persisted payload")
                            .hasMessageContaining(conflict.tableName())));
  }

  @Test
  void validatesRelationOwnershipAndRegisteredTable() {
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

    RelationTableRegistry.RelationKey unsupported =
        new RelationTableRegistry.RelationKey(
            ReleaseItemFormat.RELEASE_ITEM_FORMAT.HASH, "unsupported");
    assertThatThrownBy(() -> unsupported.value(format(1, "Vinyl")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unsupported relation key type");
  }

  private Conflict conflict(
      String tableName,
      Supplier<UpdatableRecord<?>> first,
      Supplier<UpdatableRecord<?>> second) {
    return new Conflict(tableName, first, second);
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
        .setQuantity(quantity);
  }

  private ReleaseItemTrackRecord track(String title) {
    return new ReleaseItemTrackRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(102)
        .setPosition("A1")
        .setTitle(title)
        .setDuration("3:00");
  }

  private ReleaseItemIdentifierRecord identifier(String value) {
    return new ReleaseItemIdentifierRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(103)
        .setType("Barcode")
        .setDescription("Text")
        .setValue(value);
  }

  private ReleaseItemWorkRecord work(String work) {
    return new ReleaseItemWorkRecord()
        .setReleaseItemId(RELEASE_ID)
        .setLabelId(5)
        .setHash(104)
        .setWork(work);
  }

  private ReleaseItemVideoRecord video(String title) {
    return new ReleaseItemVideoRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(105)
        .setTitle(title)
        .setDescription("Description")
        .setUrl("https://video.example");
  }

  private ReleaseItemCreditedArtistRecord creditedArtist(String role) {
    return new ReleaseItemCreditedArtistRecord()
        .setReleaseItemId(RELEASE_ID)
        .setArtistId(5)
        .setHash(106)
        .setRole(role);
  }

  private record Conflict(
      String tableName,
      Supplier<UpdatableRecord<?>> first,
      Supplier<UpdatableRecord<?>> second) {
  }
}
