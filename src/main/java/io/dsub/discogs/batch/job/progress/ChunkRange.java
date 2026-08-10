package io.dsub.discogs.batch.job.progress;

public record ChunkRange(long index, long firstItemIndex, int itemCount) {

  public ChunkRange {
    if (index < 0 || firstItemIndex < 0 || itemCount <= 0) {
      throw new IllegalArgumentException("chunk range values are out of bounds");
    }
  }
}
