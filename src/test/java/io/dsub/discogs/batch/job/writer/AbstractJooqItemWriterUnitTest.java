package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.opendiscogs.jooq.tables.records.DiscogsDumpRecord;
import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.GenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemFormatRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemImageRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemVideoRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

class AbstractJooqItemWriterUnitTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 0, 0);
  private final ExposedWriter writer = new ExposedWriter();

  @Test
  void derivesInsertFieldsAndValuesForRootAndRelationTables() {
    ArtistRecord artist =
        new ArtistRecord()
            .setId(5)
            .setCreatedAt(NOW)
            .setLastModifiedAt(NOW)
            .setName("Artist");
    ReleaseItemArtistRecord relation =
        new ReleaseItemArtistRecord()
            .setId(9)
            .setCreatedAt(NOW)
            .setLastModifiedAt(NOW)
            .setReleaseItemId(2)
            .setArtistId(5);

    assertThat(names(writer.insertFields(artist))).contains("id", "name");
    assertThat(names(writer.insertFields(relation)))
        .doesNotContain("id")
        .contains("release_item_id", "artist_id");
    assertThat(writer.insertFields(relation)).isSameAs(writer.insertFields(relation));
    assertThat(writer.insertValues(relation)).doesNotContain(9).contains(2, 5);
  }

  @Test
  void derivesRegisteredGeneratedAndRootConflictTargets() {
    ReleaseItemFormatRecord format = new ReleaseItemFormatRecord();
    ReleaseItemImageRecord image = new ReleaseItemImageRecord();
    ArtistRecord artist = new ArtistRecord();
    MasterRecord master = new MasterRecord();

    assertThat(names(writer.constraintFields(format)))
        .containsExactly("release_item_id", "hash");
    assertThat(names(writer.constraintFields(image)))
        .containsExactly("release_item_id", "hash");
    assertThatThrownBy(() -> writer.constraintFields(new DiscogsDumpRecord()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no registered canonical conflict key");
    assertThat(names(writer.constraintFields(artist))).containsExactly("id");
    assertThat(names(writer.constraintFields(master))).containsExactly("id");
    assertThat(writer.constraintFields(format)).isSameAs(writer.constraintFields(format));
  }

  @Test
  void limitsUpdatesToBusinessFieldsAndStableMetadata() {
    ArtistRecord artist =
        new ArtistRecord()
            .setId(5)
            .setCreatedAt(NOW)
            .setLastModifiedAt(NOW)
            .setName("Artist");
    MasterRecord master =
        new MasterRecord()
            .setId(7)
            .setCreatedAt(NOW)
            .setLastModifiedAt(NOW)
            .setTitle("Master")
            .setMainReleaseId(2);
    ReleaseItemVideoRecord video =
        new ReleaseItemVideoRecord()
            .setCreatedAt(NOW)
            .setLastModifiedAt(NOW)
            .setReleaseItemId(2)
            .setHash(1)
            .setUrl("https://video");
    ReleaseItemFormatRecord format =
        new ReleaseItemFormatRecord()
            .setCreatedAt(NOW)
            .setLastModifiedAt(NOW)
            .setReleaseItemId(2)
            .setHash(2)
            .setName("Vinyl")
            .setQuantity(2);

    assertThat(names(writer.updateFields(artist)))
        .contains("last_modified_at", "name")
        .doesNotContain("id", "created_at");
    assertThat(names(writer.updateFields(master))).doesNotContain("main_release_id");
    assertThat(names(writer.updateFields(video))).containsExactly("last_modified_at", "ordinal");
    assertThat(names(writer.businessUpdateFields(video))).containsExactly("ordinal");
    assertThat(names(writer.updateFields(format)))
        .containsExactly("last_modified_at", "ordinal");
    assertThat(names(writer.businessUpdateFields(format))).containsExactly("ordinal");
    assertThat(writer.updateFields(format)).isSameAs(writer.updateFields(format));
  }

  @Test
  void returnsOnlyUpdateValuesAndHandlesTablesWithoutUpdates() {
    ArtistRecord artist =
        new ArtistRecord()
            .setId(5)
            .setCreatedAt(NOW)
            .setLastModifiedAt(NOW)
            .setName("Artist");
    GenreRecord genre = new GenreRecord().setName("Rock");

    assertThat(writer.updateMap(artist))
        .containsEntry("name", "Artist")
        .containsEntry("last_modified_at", NOW)
        .doesNotContainKeys("id", "created_at");
    assertThat(writer.updateMap(artist)).doesNotContainKeys("id", "created_at");
    assertThat(writer.updateMap(genre)).isEmpty();
  }

  private List<String> names(List<Field<?>> fields) {
    return fields.stream().map(Field::getName).toList();
  }

  private static final class ExposedWriter
      extends AbstractJooqItemWriter<UpdatableRecord<?>> {

    List<Object> insertValues(UpdatableRecord<?> record) {
      return getInsertValues(record);
    }

    List<Field<?>> insertFields(UpdatableRecord<?> record) {
      return getInsertFields(record.getTable());
    }

    Map<String, Object> updateMap(UpdatableRecord<?> record) {
      return getUpdateMap(record);
    }

    List<Field<?>> constraintFields(UpdatableRecord<?> record) {
      return getConstraintFields(record.getTable());
    }

    List<Field<?>> updateFields(UpdatableRecord<?> record) {
      return getUpdateFields(record.getTable());
    }

    List<Field<?>> businessUpdateFields(UpdatableRecord<?> record) {
      return getBusinessUpdateFields(record.getTable());
    }

    @Override
    public void write(Chunk<? extends UpdatableRecord<?>> items) {
    }

    @Override
    public Query getQuery(UpdatableRecord<?> record) {
      return null;
    }
  }
}
