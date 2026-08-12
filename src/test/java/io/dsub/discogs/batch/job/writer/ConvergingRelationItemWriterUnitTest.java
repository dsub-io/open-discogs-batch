package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemTrackRecord;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

class ConvergingRelationItemWriterUnitTest {

  @Test
  void releaseLabelIdentityIncludesNullableCatalogNumber() {
    RelationTableRegistry.RelationTable labelRelease =
        RelationTableRegistry.forEntity(EntityType.RELEASE).stream()
            .filter(table -> table.table().getName().equals("label_release_item"))
            .findFirst()
            .orElseThrow();

    assertThat(labelRelease.keys())
        .extracting(key -> key.field().getName())
        .containsExactly("release_item_id", "label_id", "category_notation");
    assertThat(labelRelease.deleteStaleSql())
        .contains("category_notation is not distinct from current_keys.key_2");
  }

  @Test
  void emptySpringChunkDoesNothing() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<Collection<UpdatableRecord<?>>> delegate = mock(ItemWriter.class);
    ConvergingRelationItemWriter writer =
        new ConvergingRelationItemWriter(mock(DataSource.class), delegate);

    writer.write(new Chunk<>());

    verify(delegate, never()).write(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMixedEntityTypesBeforeOpeningADatabaseConnection() {
    @SuppressWarnings("unchecked")
    ItemWriter<Collection<UpdatableRecord<?>>> delegate = mock(ItemWriter.class);
    DataSource dataSource = mock(DataSource.class);
    ConvergingRelationItemWriter writer =
        new ConvergingRelationItemWriter(dataSource, delegate);
    Chunk<RelationSet> mixed =
        new Chunk<>(
            List.of(
                new RelationSet(EntityType.ARTIST, 1, List.of()),
                new RelationSet(EntityType.LABEL, 2, List.of())));

    assertThatThrownBy(() -> writer.write(mixed))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("multiple entity types");
  }

  @Test
  void rejectsMissingLegacyHashBeforeDatabaseAccess() {
    @SuppressWarnings("unchecked")
    ItemWriter<Collection<UpdatableRecord<?>>> delegate = mock(ItemWriter.class);
    DataSource dataSource = mock(DataSource.class);
    ConvergingRelationItemWriter writer =
        new ConvergingRelationItemWriter(dataSource, delegate);
    ReleaseItemTrackRecord incomplete = track("Track").setHash(null);

    assertThatThrownBy(
            () ->
                writer.write(
                    new Chunk<>(
                        List.of(
                            new RelationSet(
                                EntityType.RELEASE, 2, List.of(incomplete))))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("release relation identity is incomplete")
        .hasMessageContaining("release_item_track");
    verifyNoInteractions(dataSource, delegate);
  }

  private ReleaseItemTrackRecord track(String title) {
    return new ReleaseItemTrackRecord()
        .setReleaseItemId(2)
        .setHash(7)
        .setIdentitySha256(new byte[32])
        .setPosition("A1")
        .setTitle(title);
  }
}
