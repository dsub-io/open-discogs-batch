package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import java.util.Collection;
import javax.sql.DataSource;
import org.jooq.UpdatableRecord;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class DurableRelationItemWriterFactory {

  private final DataSource dataSource;
  private final ItemWriter<UpdatableRecord<?>> recordWriter;
  private final ImportProgressStore progressStore;

  public DurableRelationItemWriterFactory(
      DataSource dataSource,
      ItemWriter<UpdatableRecord<?>> recordWriter,
      ImportProgressStore progressStore) {
    this.dataSource = dataSource;
    this.recordWriter = recordWriter;
    this.progressStore = progressStore;
  }

  public ItemWriter<ProcessedChunk<RelationSet>> create(
      EntityType entityType,
      long runId,
      int chunkSize,
      boolean resumed) {
    ItemWriter<Collection<UpdatableRecord<?>>> batchedRecords =
        new CollectionItemWriter<>(recordWriter, chunkSize);
    ItemWriter<RelationSet> converging =
        new ConvergingRelationItemWriter(dataSource, batchedRecords);
    return new DurableRelationItemWriter(
        converging, progressStore, entityType, runId, chunkSize, resumed);
  }
}
