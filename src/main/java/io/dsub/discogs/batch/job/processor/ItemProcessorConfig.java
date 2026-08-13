package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.LabelRecord;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ItemProcessorConfig {

  private final DefaultEntityIdRegistry entityIdRegistry;

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<ArtistXML>, ProcessedChunk<ArtistRecord>> artistCoreProcessor() {
    return new SourceChunkItemProcessor<>(new ArtistCoreProcessor());
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<ArtistSubItemsXML>, ProcessedChunk<RelationSet>>
  artistSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(new ArtistSubItemsProcessor(entityIdRegistry));
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<LabelXML>, ProcessedChunk<LabelRecord>> labelCoreProcessor() {
    return new SourceChunkItemProcessor<>(new LabelCoreProcessor());
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<LabelSubItemsXML>, ProcessedChunk<RelationSet>>
  labelSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(new LabelSubItemsProcessor(entityIdRegistry));
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<MasterXML>, ProcessedChunk<MasterRecord>> masterCoreProcessor() {
    return new SourceChunkItemProcessor<>(new MasterCoreProcessor());
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<MasterSubItemsXML>, ProcessedChunk<RelationSet>>
  masterSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(new MasterSubItemsProcessor(entityIdRegistry));
  }

  @Bean
  @StepScope
  public ItemProcessor<SourceChunk<ReleaseItemSubItemsXML>, ProcessedChunk<ReleaseRootMutation>>
  releaseItemSubItemsProcessor() {
    return new SourceChunkItemProcessor<>(new ReleaseRootMutationProcessor(entityIdRegistry));
  }

}
