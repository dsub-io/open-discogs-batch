package io.dsub.discogs.batch.job.progress;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
import java.util.Map;

/** Immutable completed source ranges preloaded for one resumed entity. */
public record CompletedChunkInventory(EntityType entityType, Map<Long, ChunkRange> chunks) {

  public CompletedChunkInventory {
    chunks = Map.copyOf(chunks);
  }

  public boolean contains(ChunkRange source) throws ImportExecutionException {
    ChunkRange recorded = chunks.get(source.index());
    if (recorded == null) {
      return false;
    }
    if (recorded.firstItemIndex() != source.firstItemIndex()
        || recorded.itemCount() != source.itemCount()) {
      throw new ImportExecutionException(
          "recorded " + entityType + " chunk " + source.index()
              + " does not match the source range");
    }
    return true;
  }
}
