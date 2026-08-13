package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.processor.ReleaseRootMutation;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import java.util.Collection;
import javax.sql.DataSource;
import org.jooq.TableRecord;
import org.springframework.batch.infrastructure.item.ItemWriter;

public final class DurableReleaseItemWriterFactory {

  private final DataSource dataSource;
  private final ItemWriter<TableRecord<?>> recordWriter;
  private final ImportProgressStore progressStore;

  public DurableReleaseItemWriterFactory(
      DataSource dataSource,
      ItemWriter<TableRecord<?>> recordWriter,
      ImportProgressStore progressStore) {
    this.dataSource = dataSource;
    this.recordWriter = recordWriter;
    this.progressStore = progressStore;
  }

  public ItemWriter<ProcessedChunk<ReleaseRootMutation>> create(
      long runId, int chunkSize, boolean resumed) {
    ItemWriter<Collection<TableRecord<?>>> batchedRecords =
        new CollectionItemWriter<>(recordWriter, chunkSize);
    ItemWriter<RelationSet> converging =
        new ConvergingRelationItemWriter(dataSource, batchedRecords);
    return new DurableReleaseItemWriter(
        batchedRecords,
        converging,
        progressStore,
        runId,
        chunkSize,
        resumed);
  }
}
