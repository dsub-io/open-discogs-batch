package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.processor.ReleaseRootMutation;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jooq.TableRecord;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/** Commits complete Release roots and their durable source range in one transaction. */
public final class DurableReleaseItemWriter
    implements ItemWriter<ProcessedChunk<ReleaseRootMutation>> {

  private static final EntityType ENTITY_TYPE = EntityType.RELEASE;

  private final ItemWriter<Collection<TableRecord<?>>> recordWriter;
  private final ItemWriter<RelationSet> relationWriter;
  private final ImportProgressStore progressStore;
  private final long runId;
  private final int chunkSize;
  private final boolean resumed;

  public DurableReleaseItemWriter(
      ItemWriter<Collection<TableRecord<?>>> recordWriter,
      ItemWriter<RelationSet> relationWriter,
      ImportProgressStore progressStore,
      long runId,
      int chunkSize,
      boolean resumed) {
    this.recordWriter = recordWriter;
    this.relationWriter = relationWriter;
    this.progressStore = progressStore;
    this.runId = runId;
    this.chunkSize = chunkSize;
    this.resumed = resumed;
  }

  @Override
  public void write(Chunk<? extends ProcessedChunk<ReleaseRootMutation>> items) throws Exception {
    for (ProcessedChunk<ReleaseRootMutation> sourceChunk : items) {
      if (resumed && progressStore.isChunkCompleted(runId, ENTITY_TYPE, sourceChunk.range())) {
        continue;
      }

      List<TableRecord<?>> records = new ArrayList<>();
      List<RelationSet> relations = new ArrayList<>(sourceChunk.values().size());
      for (ReleaseRootMutation mutation : sourceChunk.values()) {
        records.addAll(mutation.genres());
        records.addAll(mutation.styles());
        records.add(mutation.root());
        relations.add(mutation.relations());
      }

      recordWriter.write(new Chunk<>(List.of(records)));
      relationWriter.write(new Chunk<>(relations));
      progressStore.recordCompletedChunk(runId, ENTITY_TYPE, chunkSize, sourceChunk.range());
    }
  }
}
