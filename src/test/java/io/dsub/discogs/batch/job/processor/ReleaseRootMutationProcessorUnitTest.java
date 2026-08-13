package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemArtistRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ReleaseRootMutationProcessorUnitTest {

  private static final LocalDateTime OBSERVED_AT = LocalDateTime.of(2026, 8, 1, 0, 0);

  @Test
  void buildsOneCompleteReleaseMutation() throws Exception {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    when(registry.exists(any(EntityIdRegistry.Type.class), any(Integer.class))).thenReturn(true);
    ReleaseRootMutationProcessor processor = new ReleaseRootMutationProcessor(registry);
    ReleaseItemSubItemsXML source = new ReleaseItemSubItemsXML();
    source.setId(7);
    source.setTitle(" Release ");
    source.setReleaseDate("2026-08-00");
    source.setGenres(Arrays.asList(" Rock ", "Rock", " ", null));
    source.setStyles(List.of(" House ", "House"));
    ReleaseItemSubItemsXML.ReleaseAlbumArtist artist =
        new ReleaseItemSubItemsXML.ReleaseAlbumArtist();
    artist.setArtistId(9);
    source.setReleaseAlbumArtists(List.of(artist));
    ReleaseItemSubItemsXML.ReleaseMaster master =
        new ReleaseItemSubItemsXML.ReleaseMaster();
    master.setMasterId(3);
    master.setMainRelease(true);
    source.setMaster(master);

    ReleaseRootMutation mutation = processor.process(source, OBSERVED_AT);

    assertThat(mutation.root().getId()).isEqualTo(7);
    assertThat(mutation.root().getTitle()).isEqualTo("Release");
    assertThat(mutation.root().getMasterId()).isEqualTo(3);
    assertThat(mutation.root().getCreatedAt())
        .isEqualTo(java.time.LocalDateTime.of(2026, 8, 1, 0, 0));
    assertThat(mutation.genres()).extracting("name").containsExactly("Rock");
    assertThat(mutation.styles()).extracting("name").containsExactly("House");
    assertThat(mutation.relations().rootId()).isEqualTo(7);
    assertThat((ReleaseItemArtistRecord) mutation.relations().records().get(0))
        .satisfies(
            record ->
                assertThat(record.getLastModifiedAt())
                    .isEqualTo(mutation.root().getCreatedAt()));
    assertThat(mutation.root().getIsMaster()).isTrue();
  }

  @Test
  void rejectsInvalidRootsAndLeavesNonMainRootsUnmapped() throws Exception {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    ReleaseRootMutationProcessor processor = new ReleaseRootMutationProcessor(registry);

    assertThat(processor.process(null, OBSERVED_AT)).isNull();
    assertThat(processor.process(new ReleaseItemSubItemsXML(), OBSERVED_AT)).isNull();
    ReleaseItemSubItemsXML zero = new ReleaseItemSubItemsXML();
    zero.setId(0);
    assertThat(processor.process(zero, OBSERVED_AT)).isNull();

    ReleaseItemSubItemsXML source = new ReleaseItemSubItemsXML();
    source.setId(1);
    assertThat(processor.process(source, OBSERVED_AT).root().getIsMaster()).isFalse();

    ReleaseItemSubItemsXML.ReleaseMaster master =
        new ReleaseItemSubItemsXML.ReleaseMaster();
    master.setMasterId(3);
    master.setMainRelease(false);
    source.setMaster(master);
    assertThat(processor.process(source, OBSERVED_AT).root().getIsMaster()).isFalse();
  }

  @Test
  void rejectsMissingMutationFields() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    ReleaseItemRecord root = new ReleaseItemRecord();
    RelationSet relations =
        new RelationSet(io.dsub.discogs.batch.dump.EntityType.RELEASE, 1, List.of());
    assertThatThrownBy(
            () -> new ReleaseRootMutation(null, List.of(), List.of(), relations))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(root, List.of(), List.of(), null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(root, null, List.of(), relations))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(root, List.of(), null, relations))
        .isInstanceOf(NullPointerException.class);
  }
}
