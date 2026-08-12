package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ReleaseRootMutationProcessorUnitTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void buildsOneCompleteReleaseMutation() throws Exception {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    when(registry.exists(any(EntityIdRegistry.Type.class), any(Integer.class))).thenReturn(true);
    ReleaseRootMutationProcessor processor = new ReleaseRootMutationProcessor(registry, CLOCK);
    ReleaseItemSubItemsXML source = new ReleaseItemSubItemsXML();
    source.setId(7);
    source.setTitle(" Release ");
    source.setReleaseDate("2026-08-00");
    source.setGenres(Arrays.asList(" Rock ", "Rock", " ", null));
    source.setStyles(List.of(" House ", "House"));
    ReleaseItemSubItemsXML.ReleaseMaster master =
        new ReleaseItemSubItemsXML.ReleaseMaster();
    master.setMasterId(3);
    master.setMainRelease(true);
    source.setMaster(master);

    ReleaseRootMutation mutation = processor.process(source);

    assertThat(mutation.root().getId()).isEqualTo(7);
    assertThat(mutation.root().getTitle()).isEqualTo("Release");
    assertThat(mutation.root().getMasterId()).isEqualTo(3);
    assertThat(mutation.genres()).extracting("name").containsExactly("Rock");
    assertThat(mutation.styles()).extracting("name").containsExactly("House");
    assertThat(mutation.relations().rootId()).isEqualTo(7);
    assertThat(mutation.mainReleaseAssignment().releaseId()).isEqualTo(7);
    assertThat(mutation.mainReleaseAssignment().targetMasterId()).isEqualTo(3);
    assertThat(mutation.mainReleaseAssignment().observedAt())
        .isEqualTo(java.time.LocalDateTime.of(2026, 8, 1, 0, 0));
  }

  @Test
  void rejectsInvalidRootsAndLeavesNonMainAssignmentsUnmapped() throws Exception {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    ReleaseRootMutationProcessor processor = new ReleaseRootMutationProcessor(registry, CLOCK);

    assertThat(processor.process(null)).isNull();
    assertThat(processor.process(new ReleaseItemSubItemsXML())).isNull();
    ReleaseItemSubItemsXML zero = new ReleaseItemSubItemsXML();
    zero.setId(0);
    assertThat(processor.process(zero)).isNull();

    ReleaseItemSubItemsXML source = new ReleaseItemSubItemsXML();
    source.setId(1);
    assertThat(processor.process(source).mainReleaseAssignment().targetMasterId()).isNull();

    ReleaseItemSubItemsXML.ReleaseMaster master =
        new ReleaseItemSubItemsXML.ReleaseMaster();
    master.setMasterId(3);
    master.setMainRelease(false);
    source.setMaster(master);
    assertThat(processor.process(source).mainReleaseAssignment().targetMasterId()).isNull();
  }

  @Test
  void rejectsMissingClockAndMutationFields() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    assertThatThrownBy(() -> new ReleaseRootMutationProcessor(registry, null))
        .isInstanceOf(NullPointerException.class);

    ReleaseItemRecord root = new ReleaseItemRecord();
    RelationSet relations = new RelationSet(io.dsub.discogs.batch.dump.EntityType.RELEASE, 1, List.of());
    MasterMainReleaseAssignment assignment =
        new MasterMainReleaseAssignment(1, null, java.time.LocalDateTime.MIN);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(null, List.of(), List.of(), relations, assignment))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(root, List.of(), List.of(), null, assignment))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(root, List.of(), List.of(), relations, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(root, null, List.of(), relations, assignment))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> new ReleaseRootMutation(root, List.of(), null, relations, assignment))
        .isInstanceOf(NullPointerException.class);
  }
}
