package io.dsub.discogs.batch.job.progress;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ImportProgressStore {

  private final JdbcTemplate jdbcTemplate;

  public ImportProgressStore(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public boolean isChunkCompleted(
      long runId, EntityType entityType, ChunkRange chunk)
      throws ImportExecutionException {
    List<RecordedChunk> recorded =
        jdbcTemplate.query(
            ImportProgressQueries.FIND_CHUNK,
            (result, rowNumber) -> mapRecordedChunk(result),
            runId,
            entityType.toString(),
            chunk.index());
    if (recorded.isEmpty()) {
      return false;
    }
    RecordedChunk prior = recorded.getFirst();
    if (prior.firstItemIndex() != chunk.firstItemIndex()
        || prior.itemCount() != chunk.itemCount()) {
      throw new ImportExecutionException(
          "recorded " + entityType + " chunk " + chunk.index()
              + " does not match the source range");
    }
    return true;
  }

  public void recordCompletedChunk(
      long runId, EntityType entityType, int chunkSize, ChunkRange chunk)
      throws ImportExecutionException {
    int updated =
        jdbcTemplate.update(
            ImportProgressQueries.RECORD_CHUNK,
            runId,
            entityType.toString(),
            chunk.index(),
            chunk.firstItemIndex(),
            chunk.itemCount(),
            runId,
            entityType.toString(),
            chunkSize);
    if (updated != 1) {
      throw new ImportExecutionException(
          "failed to record " + entityType + " chunk " + chunk.index()
              + ": run is inactive or progress already exists");
    }
  }

  public void fenceActiveRun(long runId) throws ImportExecutionException {
    List<Long> active =
        jdbcTemplate.query(
            ImportProgressQueries.FENCE_ACTIVE_RUN,
            (result, rowNumber) -> result.getLong(1),
            runId);
    if (active.size() != 1 || active.getFirst() != runId) {
      throw new ImportExecutionException("import run is not active: " + runId);
    }
  }

  public void completeEntity(
      long runId,
      EntityType entityType,
      int chunkSize,
      long totalItems)
      throws ImportExecutionException {
    long totalChunks = totalItems == 0 ? 0 : Math.floorDiv(totalItems - 1, chunkSize) + 1;
    int updated =
        jdbcTemplate.update(
            ImportProgressQueries.COMPLETE_ENTITY,
            runId,
            totalChunks,
            chunkSize,
            totalChunks,
            totalItems,
            chunkSize,
            runId,
            entityType.toString(),
            totalItems,
            totalChunks,
            entityType.toString(),
            chunkSize,
            totalItems,
            totalChunks,
            totalItems);
    if (updated != 1) {
      throw new ImportExecutionException(
          "chunk coverage does not match " + totalItems + " " + entityType + " items");
    }
  }

  public void completeEntityFromProgress(
      long runId, EntityType entityType, int chunkSize)
      throws ImportExecutionException {
    ProgressSummary summary =
        jdbcTemplate.queryForObject(
            ImportProgressQueries.SUMMARIZE_PROGRESS,
            (result, rowNumber) ->
                new ProgressSummary(
                    result.getLong("total_items"), result.getLong("total_chunks")),
            runId,
            entityType.toString());
    completeEntity(runId, entityType, chunkSize, summary.totalItems());
  }

  private RecordedChunk mapRecordedChunk(ResultSet result) throws SQLException {
    return new RecordedChunk(result.getLong("first_item_index"), result.getInt("item_count"));
  }

  private record RecordedChunk(long firstItemIndex, int itemCount) {
  }

  private record ProgressSummary(long totalItems, long totalChunks) {
  }
}
