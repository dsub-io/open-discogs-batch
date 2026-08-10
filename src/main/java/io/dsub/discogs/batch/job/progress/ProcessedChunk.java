package io.dsub.discogs.batch.job.progress;

import java.util.List;

public record ProcessedChunk<T>(ChunkRange range, List<T> values) {

  public ProcessedChunk {
    if (range == null) {
      throw new IllegalArgumentException("processed chunk range must not be null");
    }
    if (values == null) {
      throw new IllegalArgumentException("processed chunk values must not be null");
    }
    values = List.copyOf(values);
  }
}
