package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DiscogsDumpVerifier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.listener.IdCachingItemProcessListener;
import io.dsub.discogs.batch.job.listener.EntityProgressStepExecutionListener;
import io.dsub.discogs.batch.job.listener.ItemCountingItemProcessListener;
import io.dsub.discogs.batch.job.listener.NestedStepFailurePropagatingListener;
import io.dsub.discogs.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogs.batch.job.listener.StringNormalizingItemReadListener;
import io.dsub.discogs.batch.job.step.AbstractStepConfig;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import io.dsub.discogs.batch.job.reader.SourceChunkItemStreamReader;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.job.writer.DurableRelationItemWriterFactory;
import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.UpdatableRecord;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistStepConfig extends AbstractStepConfig {

  public static final String ARTIST_STEP_FLOW = "artist step flow";
  public static final String ARTIST_FLOW_STEP = "artist flow step";
  public static final String ARTIST_CORE_INSERTION_STEP = "artist core insertion step";
  public static final String ARTIST_SUB_ITEMS_INSERTION_STEP = "artist sub items insertion step";
  public static final String ARTIST_FILE_FETCH_STEP = "artist file fetch step";
  public static final String ARTIST_FILE_CLEAR_STEP = "artist file clear step";

  private final SynchronizedItemStreamReader<ArtistXML> artistStreamReader;
  private final SourceChunkItemStreamReader<ArtistSubItemsXML> artistSubItemsStreamReader;
  private final ItemProcessor<SourceChunk<ArtistSubItemsXML>, ProcessedChunk<RelationSet>>
      artistSubItemsProcessor;
  private final ItemProcessor<ArtistXML, ArtistRecord> artistCoreProcessor;
  private final ItemWriter<UpdatableRecord<?>> entityItemWriter;
  private final DurableRelationItemWriterFactory durableRelationItemWriterFactory;
  private final ImportProgressStore importProgressStore;
  private final DiscogsDump artistDump;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final FileUtil fileUtil;
  private final DiscogsDumpVerifier dumpVerifier;

  private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
  private final StringNormalizingItemReadListener stringNormalizingItemReadListener;
  private final IdCachingItemProcessListener idCachingItemProcessListener;
  private final ItemCountingItemProcessListener itemCountingItemProcessListener;

  @Bean
  @JobScope
  public Step artistStep() throws InvalidArgumentException, DumpNotFoundException {

    // @formatter:off
    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(ARTIST_STEP_FLOW)

            // execution decider
            .from(executionDecider(ARTIST))
            .on(SKIPPED)
            .end()
            .on(ANY)
            .to(artistFileFetchStep())

            // from fetch
            .from(artistFileFetchStep())
            .on(FAILED)
            .fail()
            .from(artistFileFetchStep())
            .on(ANY)
            .to(artistCoreInsertionStep(null))

            // from core insert
            .from(artistCoreInsertionStep(null))
            .on(FAILED)
            .fail()
            .from(artistCoreInsertionStep(null))
            .on(ANY)
            .to(artistSubItemsInsertionStep(null, null, null))

            // from sub items insert
            .from(artistSubItemsInsertionStep(null, null, null))
            .on(FAILED)
            .fail()
            .from(artistSubItemsInsertionStep(null, null, null))
            .on(ANY)
            .end()

            // conclude
            .build();
    // @formatter:on

    FlowStep artistFlowStep = new FlowStep(jobRepository);
    artistFlowStep.setName(ARTIST_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    artistFlowStep.registerStepExecutionListener(new NestedStepFailurePropagatingListener());
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step artistCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return new StepBuilder(ARTIST_CORE_INSERTION_STEP, jobRepository)
        .<ArtistXML, UpdatableRecord<?>>chunk(chunkSize)
        .transactionManager(transactionManager)
        .reader(artistStreamReader)
        .processor(artistCoreProcessor)
        .writer(entityItemWriter)
        .faultTolerant()
        .retryLimit(100)
        .retry(PessimisticLockingFailureException.class)
        .listener(stopWatchStepExecutionListener)
        .listener(stringNormalizingItemReadListener)
        .listener(idCachingItemProcessListener)
        .listener(itemCountingItemProcessListener)
        .taskExecutor(taskExecutor)
        .allowStartIfComplete(true)
        .build();
  }

  @Bean
  @JobScope
  public Step artistSubItemsInsertionStep(
      @Value(CHUNK) Integer chunkSize,
      @Value(RUN_ID) Long runId,
      @Value(RESUMED) Boolean resumed) {
    return new StepBuilder(ARTIST_SUB_ITEMS_INSERTION_STEP, jobRepository)
        .<SourceChunk<ArtistSubItemsXML>, ProcessedChunk<RelationSet>>chunk(
            TRACKED_CHUNKS_PER_TRANSACTION)
        .transactionManager(transactionManager)
        .reader(artistSubItemsStreamReader)
        .processor(artistSubItemsProcessor)
        .writer(
            durableRelationItemWriterFactory.create(
                EntityType.ARTIST, runId, chunkSize, resumed))
        .faultTolerant()
        .retryLimit(100)
        .retry(PessimisticLockingFailureException.class)
        .listener(stringNormalizingItemReadListener)
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .listener(
            new EntityProgressStepExecutionListener(
                importProgressStore, EntityType.ARTIST, runId, chunkSize, resumed))
        .taskExecutor(taskExecutor)
        .allowStartIfComplete(true)
        .build();
  }

  @Bean
  @JobScope
  public Step artistFileFetchStep() throws DumpNotFoundException {
    return new StepBuilder(ARTIST_FILE_FETCH_STEP, jobRepository)
        .tasklet(
            new FileFetchTasklet(artistDump, fileUtil, dumpVerifier), transactionManager)
        .build();
  }
}
