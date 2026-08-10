package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/** Commits canonical relation changes and their durable source range in one transaction. */
public class DurableRelationItemWriter
    implements ItemWriter<ProcessedChunk<RelationSet>> {

  private final ItemWriter<RelationSet> delegate;
  private final ImportProgressStore progressStore;
  private final EntityType entityType;
  private final long runId;
  private final int chunkSize;
  private final boolean resumed;

  public DurableRelationItemWriter(
      ItemWriter<RelationSet> delegate,
      ImportProgressStore progressStore,
      EntityType entityType,
      long runId,
      int chunkSize,
      boolean resumed) {
    this.delegate = delegate;
    this.progressStore = progressStore;
    this.entityType = entityType;
    this.runId = runId;
    this.chunkSize = chunkSize;
    this.resumed = resumed;
  }

  @Override
  public void write(Chunk<? extends ProcessedChunk<RelationSet>> items) throws Exception {
    for (ProcessedChunk<RelationSet> sourceChunk : items) {
      if (resumed
          && progressStore.isChunkCompleted(runId, entityType, sourceChunk.range())) {
        continue;
      }
      delegate.write(new Chunk<>(sourceChunk.values()));
      progressStore.recordCompletedChunk(
          runId, entityType, chunkSize, sourceChunk.range());
    }
  }
}
