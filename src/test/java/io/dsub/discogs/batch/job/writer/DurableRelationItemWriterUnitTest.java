package io.dsub.discogs.batch.job.writer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

class DurableRelationItemWriterUnitTest {

  private static final long RUN_ID = 7L;
  private static final int CHUNK_SIZE = 5;

  @Test
  void freshChunkWritesAndRecordsWithoutResumeLookup() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<RelationSet> delegate = mock(ItemWriter.class);
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    DurableRelationItemWriter writer =
        new DurableRelationItemWriter(
            delegate, progressStore, EntityType.ARTIST, RUN_ID, CHUNK_SIZE, false);
    ProcessedChunk<RelationSet> processed = processedChunk();

    writer.write(new Chunk<>(List.of(processed)));

    verify(delegate).write(new Chunk<>(processed.values()));
    verify(progressStore, never())
        .isChunkCompleted(RUN_ID, EntityType.ARTIST, processed.range());
    verify(progressStore)
        .recordCompletedChunk(
            RUN_ID, EntityType.ARTIST, CHUNK_SIZE, processed.range());
  }

  @Test
  void resumedCompletedChunkSkipsCanonicalWriteAndDuplicateLedger() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<RelationSet> delegate = mock(ItemWriter.class);
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    DurableRelationItemWriter writer =
        new DurableRelationItemWriter(
            delegate, progressStore, EntityType.ARTIST, RUN_ID, CHUNK_SIZE, true);
    ProcessedChunk<RelationSet> processed = processedChunk();
    when(progressStore.isChunkCompleted(RUN_ID, EntityType.ARTIST, processed.range()))
        .thenReturn(true);

    writer.write(new Chunk<>(List.of(processed)));

    verify(delegate, never()).write(new Chunk<>(processed.values()));
    verify(progressStore, never())
        .recordCompletedChunk(
            RUN_ID, EntityType.ARTIST, CHUNK_SIZE, processed.range());
  }

  @Test
  void resumedMissingChunkWritesAndRecordsNormally() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<RelationSet> delegate = mock(ItemWriter.class);
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    DurableRelationItemWriter writer =
        new DurableRelationItemWriter(
            delegate, progressStore, EntityType.ARTIST, RUN_ID, CHUNK_SIZE, true);
    ProcessedChunk<RelationSet> processed = processedChunk();
    when(progressStore.isChunkCompleted(RUN_ID, EntityType.ARTIST, processed.range()))
        .thenReturn(false);

    writer.write(new Chunk<>(List.of(processed)));

    verify(delegate).write(new Chunk<>(processed.values()));
    verify(progressStore)
        .recordCompletedChunk(
            RUN_ID, EntityType.ARTIST, CHUNK_SIZE, processed.range());
  }

  private ProcessedChunk<RelationSet> processedChunk() {
    RelationSet relationSet = new RelationSet(EntityType.ARTIST, 1, List.of());
    return new ProcessedChunk<>(
        new ChunkRange(0, 0, 1), List.of(relationSet));
  }
}
