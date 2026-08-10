package io.dsub.discogs.batch.job.reader;

import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/** Emits bounded, contiguous source windows that remain stable under concurrent processing. */
public class SourceChunkItemStreamReader<T> implements ItemStreamReader<SourceChunk<T>> {

  private final ItemStreamReader<T> delegate;
  private final int sourceChunkSize;
  private long nextItemIndex;
  private long nextChunkIndex;

  public SourceChunkItemStreamReader(ItemStreamReader<T> delegate, int sourceChunkSize) {
    if (sourceChunkSize <= 0) {
      throw new IllegalArgumentException("source chunk size must be positive");
    }
    this.delegate = delegate;
    this.sourceChunkSize = sourceChunkSize;
  }

  @Override
  public synchronized SourceChunk<T> read() throws Exception {
    List<T> values = new ArrayList<>(sourceChunkSize);
    while (values.size() < sourceChunkSize) {
      T value = delegate.read();
      if (value == null) {
        break;
      }
      values.add(value);
    }
    if (values.isEmpty()) {
      return null;
    }
    ChunkRange range = new ChunkRange(nextChunkIndex, nextItemIndex, values.size());
    nextChunkIndex++;
    nextItemIndex = Math.addExact(nextItemIndex, values.size());
    return new SourceChunk<>(range, values);
  }

  @Override
  public synchronized void open(ExecutionContext executionContext) throws ItemStreamException {
    nextItemIndex = 0;
    nextChunkIndex = 0;
    delegate.open(executionContext);
  }

  @Override
  public synchronized void update(ExecutionContext executionContext) throws ItemStreamException {
    delegate.update(executionContext);
  }

  @Override
  public synchronized void close() throws ItemStreamException {
    delegate.close();
  }
}
