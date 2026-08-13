package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.ArtistNameVariation;
import io.dsub.opendiscogs.jooq.tables.ArtistUrl;
import io.dsub.opendiscogs.jooq.tables.LabelUrl;
import io.dsub.opendiscogs.jooq.tables.MasterVideo;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemTrackRecord;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.jooq.TableRecord;
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
    assertThat(labelRelease.deleteStaleSql(2))
        .contains("(?, ?, ?), (?, ?, ?)")
        .contains("category_notation is not distinct from current_keys.key_2");
    assertThatThrownBy(() -> labelRelease.deleteStaleSql(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("row count");
  }

  @Test
  void nonReleaseHashRelationsUseDigestForSemanticAndStaleIdentity() {
    assertHashRelation(
        EntityType.ARTIST, ArtistNameVariation.ARTIST_NAME_VARIATION, "artist_id");
    assertHashRelation(EntityType.ARTIST, ArtistUrl.ARTIST_URL, "artist_id");
    assertHashRelation(EntityType.LABEL, LabelUrl.LABEL_URL, "label_id");
    assertHashRelation(EntityType.MASTER, MasterVideo.MASTER_VIDEO, "master_id");
  }

  @Test
  void emptySpringChunkDoesNothing() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<Collection<TableRecord<?>>> delegate = mock(ItemWriter.class);
    ConvergingRelationItemWriter writer =
        new ConvergingRelationItemWriter(mock(DataSource.class), delegate);

    writer.write(new Chunk<>());

    verify(delegate, never()).write(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void emptyRootLookupDoesNotOpenADatabaseConnection() {
    DataSource dataSource = mock(DataSource.class);
    ExistingRelationRoots roots =
        new ExistingRelationRootsReader(dataSource).find(EntityType.ARTIST, Set.of());

    assertThat(
            roots.forTable(RelationTableRegistry.forEntity(EntityType.ARTIST).getFirst()))
        .isEmpty();
    verifyNoInteractions(dataSource);
  }

  @Test
  void rejectsMixedEntityTypesBeforeOpeningADatabaseConnection() {
    @SuppressWarnings("unchecked")
    ItemWriter<Collection<TableRecord<?>>> delegate = mock(ItemWriter.class);
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
    ItemWriter<Collection<TableRecord<?>>> delegate = mock(ItemWriter.class);
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
        .hasMessageContaining("relation identity is incomplete")
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

  private void assertHashRelation(EntityType entityType, org.jooq.Table<?> table, String ownerKey) {
    RelationTableRegistry.RelationTable relation =
        RelationTableRegistry.require(entityType, table);
    assertThat(relation.keys())
        .extracting(key -> key.field().getName())
        .containsExactly(ownerKey, "hash", "identity_sha256");
    assertThat(relation.conflictFields())
        .extracting(org.jooq.Field::getName)
        .containsExactly(ownerKey, "hash");
    assertThat(relation.deleteStaleSql(1))
        .contains("(?, ?, ?)")
        .contains("identity_sha256 is not distinct from current_keys.key_2");
  }
}
