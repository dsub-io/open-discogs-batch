package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/** Rolls back a chunk when its owning import run is no longer active. */
public class ActiveRunItemWriter<T> implements ItemWriter<T> {

  private final ItemWriter<T> delegate;
  private final ImportProgressStore progressStore;
  private final long runId;

  public ActiveRunItemWriter(
      ItemWriter<T> delegate, ImportProgressStore progressStore, long runId) {
    this.delegate = delegate;
    this.progressStore = progressStore;
    this.runId = runId;
  }

  @Override
  public void write(Chunk<? extends T> items) throws Exception {
    delegate.write(items);
    progressStore.fenceActiveRun(runId);
  }
}
