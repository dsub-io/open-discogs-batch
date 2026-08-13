package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/** Writes each bounded source chunk as one delegate batch. */
public final class ProcessedChunkItemWriter<T> implements ItemWriter<ProcessedChunk<T>> {

  private final ItemWriter<? super T> delegate;

  public ProcessedChunkItemWriter(ItemWriter<? super T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void write(Chunk<? extends ProcessedChunk<T>> items) throws Exception {
    for (ProcessedChunk<T> processedChunk : items) {
      delegate.write(new Chunk<>(processedChunk.values()));
    }
  }
}
