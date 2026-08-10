package io.dsub.discogs.batch.job.progress;

import java.util.List;

public record SourceChunk<T>(ChunkRange range, List<T> values) {

  public SourceChunk {
    if (range == null) {
      throw new IllegalArgumentException("source chunk range must not be null");
    }
    if (values == null) {
      throw new IllegalArgumentException("source chunk values must not be null");
    }
    values = List.copyOf(values);
    if (values.size() != range.itemCount()) {
      throw new IllegalArgumentException("source chunk values do not match its range");
    }
  }
}
