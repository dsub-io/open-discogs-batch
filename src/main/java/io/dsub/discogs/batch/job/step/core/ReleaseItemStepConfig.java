package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DiscogsDumpVerifier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.decider.MasterMainReleaseStepJobExecutionDecider;
import io.dsub.discogs.batch.job.listener.EntityProgressStepExecutionListener;
import io.dsub.discogs.batch.job.listener.IdCachingItemProcessListener;
import io.dsub.discogs.batch.job.listener.ItemCountingItemProcessListener;
import io.dsub.discogs.batch.job.listener.NestedStepFailurePropagatingListener;
import io.dsub.discogs.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogs.batch.job.listener.StringNormalizingItemReadListener;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import io.dsub.discogs.batch.job.reader.SourceChunkItemStreamReader;
import io.dsub.discogs.batch.job.step.AbstractStepConfig;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.job.tasklet.GenreStyleInsertionTasklet;
import io.dsub.discogs.batch.job.writer.DurableRelationItemWriterFactory;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
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
public class ReleaseItemStepConfig extends AbstractStepConfig {

  public static final String RELEASE_STEP_FLOW = "release item step flow";
  public static final String RELEASE_FLOW_STEP = "release item flow step";
  public static final String RELEASE_ITEM_CORE_INSERTION_STEP = "release item core insertion step";
  public static final String RELEASE_ITEM_SUB_ITEMS_INSERTION_STEP =
      "release item sub items insertion step";
  public static final String RELEASE_FILE_FETCH_STEP = "release item file fetch step";
  public static final String MASTER_MAIN_RELEASE_UPDATE_STEP = "master main release update step";
  public static final String RELEASE_GENRE_STYLE_INSERTION_STEP =
      "release genre style insertion step";

  private final SourceChunkItemStreamReader<ReleaseItemSubItemsXML>
      releaseItemSubItemsStreamReader;
  private final SynchronizedItemStreamReader<ReleaseItemXML> releaseItemStreamReader;
  private final SynchronizedItemStreamReader<MasterMainReleaseXML> masterMainReleaseStreamReader;

  private final ItemProcessor<
          SourceChunk<ReleaseItemSubItemsXML>, ProcessedChunk<RelationSet>>
      releaseItemSubItemsProcessor;
  private final ItemProcessor<ReleaseItemXML, ReleaseItemRecord> releaseItemCoreProcessor;
  private final ItemProcessor<MasterMainReleaseXML, MasterRecord> masterMainReleaseItemProcessor;

  private final ItemWriter<UpdatableRecord<?>> entityItemWriter;
  private final DurableRelationItemWriterFactory durableRelationItemWriterFactory;
  private final ImportProgressStore importProgressStore;
  private final ItemWriter<MasterRecord> postgresJooqMasterMainReleaseItemWriter;

  private final DiscogsDump releaseItemDump;

  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final FileUtil fileUtil;
  private final DiscogsDumpVerifier dumpVerifier;
  private final GenreStyleInsertionTasklet genreStyleInsertionTasklet;

  private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
  private final StringNormalizingItemReadListener stringNormalizingItemReadListener;
  private final ItemCountingItemProcessListener itemCountingItemProcessListener;
  private final IdCachingItemProcessListener idCachingItemProcessListener;
  private final MasterMainReleaseStepJobExecutionDecider masterMainReleaseStepJobExecutionDecider;

  @Bean
  @JobScope
  public Step releaseStep(@Value(CHUNK) Integer chunkSize)
      throws InvalidArgumentException, DumpNotFoundException {
    // @formatter:off
    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(RELEASE_STEP_FLOW)

            // from execution decider
            .from(executionDecider(RELEASE))
            .on(SKIPPED)
            .end()
            .on(ANY)
            .to(releaseFileFetchStep())

            // from fetch
            .from(releaseFileFetchStep())
            .on(FAILED)
            .fail()
            .from(releaseFileFetchStep())
            .on(ANY)
            .to(releaseItemCoreInsertionStep(chunkSize))

            // from core insertion
            .from(releaseItemCoreInsertionStep(chunkSize))
            .on(FAILED)
            .fail()
            .from(releaseItemCoreInsertionStep(chunkSize))
            .on(ANY)
            .to(releaseGenreStyleInsertionStep())

            // from genre style insertion step
            .from(releaseGenreStyleInsertionStep())
            .on(FAILED)
            .fail()
            .from(releaseGenreStyleInsertionStep())
            .on(ANY)
            .to(releaseItemSubItemsInsertionStep(chunkSize, null, null))

            // from sub items insertion
            .from(releaseItemSubItemsInsertionStep(chunkSize, null, null))
            .on(FAILED)
            .fail()
            .from(releaseItemSubItemsInsertionStep(chunkSize, null, null))
            .on(ANY)
            .to(masterMainReleaseStepJobExecutionDecider)

            // from master main release step decider
            .from(masterMainReleaseStepJobExecutionDecider)
            .on(SKIPPED)
            .end()
            .from(masterMainReleaseStepJobExecutionDecider)
            .on(ANY)
            .to(masterMainReleaseUpdateStep(chunkSize))

            // from master main release update step
            .from(masterMainReleaseUpdateStep(chunkSize))
            .on(FAILED)
            .fail()
            .from(masterMainReleaseUpdateStep(chunkSize))
            .on(ANY)
            .end()
            // conclude
            .build();
    // @formatter:on

    FlowStep artistFlowStep = new FlowStep(jobRepository);
    artistFlowStep.setName(RELEASE_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    artistFlowStep.registerStepExecutionListener(new NestedStepFailurePropagatingListener());
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step releaseItemCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return new StepBuilder(RELEASE_ITEM_CORE_INSERTION_STEP, jobRepository)
        .<ReleaseItemXML, UpdatableRecord<?>>chunk(chunkSize)
        .transactionManager(transactionManager)
        .reader(releaseItemStreamReader)
        .processor(releaseItemCoreProcessor)
        .writer(entityItemWriter)
        .faultTolerant()
        .retryLimit(100)
        .retry(PessimisticLockingFailureException.class)
        .listener(stopWatchStepExecutionListener)
        .listener(stringNormalizingItemReadListener)
        .listener(itemCountingItemProcessListener)
        .listener(idCachingItemProcessListener)
        .taskExecutor(taskExecutor)
        .build();
  }

  @Bean
  @JobScope
  public Step releaseItemSubItemsInsertionStep(
      @Value(CHUNK) Integer chunkSize,
      @Value(RUN_ID) Long runId,
      @Value(RESUMED) Boolean resumed) {
    return new StepBuilder(RELEASE_ITEM_SUB_ITEMS_INSERTION_STEP, jobRepository)
        .<SourceChunk<ReleaseItemSubItemsXML>, ProcessedChunk<RelationSet>>chunk(
            TRACKED_CHUNKS_PER_TRANSACTION)
        .transactionManager(transactionManager)
        .reader(releaseItemSubItemsStreamReader)
        .processor(releaseItemSubItemsProcessor)
        .writer(
            durableRelationItemWriterFactory.create(
                EntityType.RELEASE, runId, chunkSize, resumed))
        .faultTolerant()
        .retryLimit(100)
        .retry(PessimisticLockingFailureException.class)
        .listener(stringNormalizingItemReadListener)
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .listener(
            new EntityProgressStepExecutionListener(
                importProgressStore, EntityType.RELEASE, runId, chunkSize, resumed))
        .taskExecutor(taskExecutor)
        .build();
  }

  @Bean
  @JobScope
  public Step releaseFileFetchStep() throws DumpNotFoundException {
    return new StepBuilder(RELEASE_FILE_FETCH_STEP, jobRepository)
        .tasklet(
            new FileFetchTasklet(releaseItemDump, fileUtil, dumpVerifier), transactionManager)
        .build();
  }

  @Bean
  @JobScope
  public Step masterMainReleaseUpdateStep(@Value(CHUNK) Integer chunkSize) {
    return new StepBuilder(MASTER_MAIN_RELEASE_UPDATE_STEP, jobRepository)
        .<MasterMainReleaseXML, MasterRecord>chunk(chunkSize)
        .transactionManager(transactionManager)
        .reader(masterMainReleaseStreamReader)
        .processor(masterMainReleaseItemProcessor)
        .writer(postgresJooqMasterMainReleaseItemWriter)
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .taskExecutor(taskExecutor)
        .build();
  }

  @Bean
  @JobScope
  public Step releaseGenreStyleInsertionStep() {
    return new StepBuilder(RELEASE_GENRE_STYLE_INSERTION_STEP, jobRepository)
        .tasklet(genreStyleInsertionTasklet, transactionManager)
        .build();
  }
}
