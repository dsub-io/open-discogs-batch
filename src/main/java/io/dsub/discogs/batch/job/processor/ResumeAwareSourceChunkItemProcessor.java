package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.job.progress.CompletedChunkInventory;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/** Skips preloaded completed source ranges before domain transformation. */
public final class ResumeAwareSourceChunkItemProcessor<I, O>
    implements ItemProcessor<SourceChunk<I>, ProcessedChunk<O>> {

  private final ItemProcessor<SourceChunk<I>, ProcessedChunk<O>> delegate;
  private final CompletedChunkInventory completedChunks;

  public ResumeAwareSourceChunkItemProcessor(
      ItemProcessor<SourceChunk<I>, ProcessedChunk<O>> delegate,
      CompletedChunkInventory completedChunks) {
    this.delegate = delegate;
    this.completedChunks = completedChunks;
  }

  @Override
  public ProcessedChunk<O> process(SourceChunk<I> source) throws Exception {
    if (completedChunks.contains(source.range())) {
      return null;
    }
    return delegate.process(source);
  }
}
