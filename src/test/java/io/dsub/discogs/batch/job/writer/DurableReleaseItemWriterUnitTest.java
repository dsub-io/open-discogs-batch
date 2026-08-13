package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.processor.ReleaseRootMutation;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.opendiscogs.jooq.tables.records.GenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.StyleRecord;
import java.util.Collection;
import java.util.List;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

class DurableReleaseItemWriterUnitTest {

  private static final long RUN_ID = 9L;
  private static final int CHUNK_SIZE = 2;
  private static final ChunkRange RANGE = new ChunkRange(0, 0, 1);

  @Test
  @SuppressWarnings("unchecked")
  void writesTheCompleteMutationBeforeRecordingProgress() throws Exception {
    Fixture fixture = fixture(false);
    ReleaseRootMutation mutation = mutation();

    fixture.writer().write(new Chunk<>(List.of(new ProcessedChunk<>(RANGE, List.of(mutation)))));

    ArgumentCaptor<Chunk<? extends Collection<UpdatableRecord<?>>>> records =
        ArgumentCaptor.forClass(Chunk.class);
    verify(fixture.recordWriter()).write(records.capture());
    assertThat(records.getValue().getItems().getFirst())
        .containsExactlyElementsOf(
            List.of(mutation.genres().getFirst(), mutation.styles().getFirst(), mutation.root()));
    verify(fixture.relationWriter()).write(any());
    verify(fixture.progressStore())
        .recordCompletedChunk(RUN_ID, EntityType.RELEASE, CHUNK_SIZE, RANGE);
    verify(fixture.progressStore(), never()).isChunkCompleted(anyLong(), any(), any());
  }

  @Test
  void skipsOnlyAlreadyCompletedChunksOnResume() throws Exception {
    Fixture completed = fixture(true);
    when(completed.progressStore().isChunkCompleted(RUN_ID, EntityType.RELEASE, RANGE))
        .thenReturn(true);

    completed.writer().write(
        new Chunk<>(List.of(new ProcessedChunk<>(RANGE, List.of(mutation())))));

    verify(completed.recordWriter(), never()).write(any());
    verify(completed.relationWriter(), never()).write(any());
    verify(completed.progressStore(), never())
        .recordCompletedChunk(anyLong(), any(), anyInt(), any());

    Fixture incomplete = fixture(true);
    when(incomplete.progressStore().isChunkCompleted(RUN_ID, EntityType.RELEASE, RANGE))
        .thenReturn(false);
    incomplete.writer().write(
        new Chunk<>(List.of(new ProcessedChunk<>(RANGE, List.of()))));
    verify(incomplete.recordWriter()).write(any());
    verify(incomplete.progressStore())
        .recordCompletedChunk(RUN_ID, EntityType.RELEASE, CHUNK_SIZE, RANGE);
  }

  @SuppressWarnings("unchecked")
  private Fixture fixture(boolean resumed) {
    ItemWriter<Collection<UpdatableRecord<?>>> recordWriter = mock(ItemWriter.class);
    ItemWriter<RelationSet> relationWriter = mock(ItemWriter.class);
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    DurableReleaseItemWriter writer =
        new DurableReleaseItemWriter(
            recordWriter,
            relationWriter,
            progressStore,
            RUN_ID,
            CHUNK_SIZE,
            resumed);
    return new Fixture(writer, recordWriter, relationWriter, progressStore);
  }

  private ReleaseRootMutation mutation() {
    ReleaseItemRecord root = new ReleaseItemRecord().setId(1);
    RelationSet relations = new RelationSet(EntityType.RELEASE, 1, List.of());
    return new ReleaseRootMutation(
        root,
        List.of(new GenreRecord().setName("Rock")),
        List.of(new StyleRecord().setName("House")),
        relations);
  }

  private record Fixture(
      DurableReleaseItemWriter writer,
      ItemWriter<Collection<UpdatableRecord<?>>> recordWriter,
      ItemWriter<RelationSet> relationWriter,
      ImportProgressStore progressStore) {
  }
}
