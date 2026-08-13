package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.job.ImportJobParameters;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.TransactionBoundaries;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.LabelRecord;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

@Configuration
public class ItemProcessorConfig {

  private final DefaultEntityIdRegistry entityIdRegistry;
  private final TransactionOperations processingOperations;

  public ItemProcessorConfig(
      DefaultEntityIdRegistry entityIdRegistry,
      PlatformTransactionManager transactionManager) {
    this.entityIdRegistry = entityIdRegistry;
    this.processingOperations = TransactionBoundaries.suspended(transactionManager);
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<ArtistXML>, ProcessedChunk<ArtistRecord>> artistCoreProcessor() {
    return new SourceChunkItemProcessor<>(new ArtistCoreProcessor(), processingOperations);
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<ArtistSubItemsXML>, ProcessedChunk<RelationSet>>
  artistSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(
        new ArtistSubItemsProcessor(entityIdRegistry), processingOperations);
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<LabelXML>, ProcessedChunk<LabelRecord>> labelCoreProcessor() {
    return new SourceChunkItemProcessor<>(new LabelCoreProcessor(), processingOperations);
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<LabelSubItemsXML>, ProcessedChunk<RelationSet>>
  labelSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(
        new LabelSubItemsProcessor(entityIdRegistry), processingOperations);
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<MasterXML>, ProcessedChunk<MasterRecord>> masterCoreProcessor(
      ImportProgressStore progressStore,
      @Value("#{jobParameters['" + ImportJobParameters.RUN_ID + "']}") Long runId) {
    return new SourceChunkItemProcessor<>(
        new MasterCoreProcessor(progressStore.shouldSeedMasterMainReleases(runId)),
        processingOperations);
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<MasterSubItemsXML>, ProcessedChunk<RelationSet>>
  masterSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(
        new MasterSubItemsProcessor(entityIdRegistry), processingOperations);
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<ReleaseItemSubItemsXML>, ProcessedChunk<ReleaseRootMutation>>
  releaseItemSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(
        new ReleaseRootMutationProcessor(entityIdRegistry), processingOperations);
  }

}
