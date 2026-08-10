package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
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
import io.dsub.discogs.batch.job.tasklet.GenreStyleInsertionTasklet;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.job.writer.DurableRelationItemWriterFactory;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
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
public class MasterStepConfig extends AbstractStepConfig {

  public static final String MASTER_STEP_FLOW = "master step flow";
  public static final String MASTER_FLOW_STEP = "master flow step";
  public static final String MASTER_CORE_INSERTION_STEP = "master core insertion step";
  public static final String MASTER_SUB_ITEMS_INSERTION_STEP = "master sub items insertion step";
  public static final String MASTER_FILE_FETCH_STEP = "master file fetch step";
  public static final String MASTER_FILE_CLEAR_STEP = "master file clear step";
  public static final String MASTER_GENRE_STYLE_INSERTION_STEP =
      "master genre style insertion step";

  private final SynchronizedItemStreamReader<MasterXML> masterStreamReader;
  private final SourceChunkItemStreamReader<MasterSubItemsXML> masterSubItemsStreamReader;
  private final ItemProcessor<MasterXML, MasterRecord> masterCoreProcessor;
  private final ItemProcessor<SourceChunk<MasterSubItemsXML>, ProcessedChunk<RelationSet>>
      masterSubItemsProcessor;
  private final DurableRelationItemWriterFactory durableRelationItemWriterFactory;
  private final ImportProgressStore importProgressStore;
  private final ItemWriter<UpdatableRecord<?>> entityItemWriter;
  private final DiscogsDump masterDump;

  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final FileUtil fileUtil;
  private final DiscogsDumpVerifier dumpVerifier;
  private final GenreStyleInsertionTasklet genreStyleInsertionTasklet;

  private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
  private final StringNormalizingItemReadListener stringNormalizingItemReadListener;
  private final IdCachingItemProcessListener idCachingItemProcessListener;
  private final ItemCountingItemProcessListener itemCountingItemProcessListener;

  @Bean
  @JobScope
  public Step masterStep(@Value(CHUNK) Integer chunkSize)
      throws InvalidArgumentException, DumpNotFoundException {

    // @formatter:off
    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(MASTER_STEP_FLOW)

            // from execution decider
            .from(executionDecider(MASTER))
            .on(SKIPPED)
            .end()
            .on(ANY)
            .to(masterFileFetchStep())

            // from fetch
            .from(masterFileFetchStep())
            .on(FAILED)
            .fail()
            .from(masterFileFetchStep())
            .on(ANY)
            .to(masterCoreInsertionStep(chunkSize))

            // from core insertion
            .from(masterCoreInsertionStep(chunkSize))
            .on(FAILED)
            .fail()
            .from(masterCoreInsertionStep(chunkSize))
            .on(ANY)
            .to(masterGenreStyleInsertionStep())

            // from master genre style insertion step
            .from(masterGenreStyleInsertionStep())
            .on(FAILED)
            .fail()
            .from(masterGenreStyleInsertionStep())
            .on(ANY)
            .to(masterSubItemsInsertionStep(chunkSize, null, null))

            // from sub items insertion
            .from(masterSubItemsInsertionStep(chunkSize, null, null))
            .on(FAILED)
            .fail()
            .from(masterSubItemsInsertionStep(chunkSize, null, null))
            .on(ANY)
            .end()

            // conclude
            .build();
    // @formatter:on

    FlowStep artistFlowStep = new FlowStep(jobRepository);
    artistFlowStep.setName(MASTER_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    artistFlowStep.registerStepExecutionListener(new NestedStepFailurePropagatingListener());
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step masterFileFetchStep() throws DumpNotFoundException {
    return new StepBuilder(MASTER_FILE_FETCH_STEP, jobRepository)
        .tasklet(
            new FileFetchTasklet(masterDump, fileUtil, dumpVerifier), transactionManager)
        .build();
  }

  @Bean
  @JobScope
  public Step masterCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return new StepBuilder(MASTER_CORE_INSERTION_STEP, jobRepository)
        .<MasterXML, UpdatableRecord<?>>chunk(chunkSize)
        .transactionManager(transactionManager)
        .reader(masterStreamReader)
        .processor(masterCoreProcessor)
        .writer(entityItemWriter)
        .faultTolerant()
        .retryLimit(10)
        .retry(PessimisticLockingFailureException.class)
        .listener(stopWatchStepExecutionListener)
        .listener(stringNormalizingItemReadListener)
        .listener(idCachingItemProcessListener)
        .listener(itemCountingItemProcessListener)
        .taskExecutor(taskExecutor)
        .build();
  }

  @Bean
  @JobScope
  public Step masterSubItemsInsertionStep(
      @Value(CHUNK) Integer chunkSize,
      @Value(RUN_ID) Long runId,
      @Value(RESUMED) Boolean resumed) {
    return new StepBuilder(MASTER_SUB_ITEMS_INSERTION_STEP, jobRepository)
        .<SourceChunk<MasterSubItemsXML>, ProcessedChunk<RelationSet>>chunk(
            TRACKED_CHUNKS_PER_TRANSACTION)
        .transactionManager(transactionManager)
        .reader(masterSubItemsStreamReader)
        .processor(masterSubItemsProcessor)
        .writer(
            durableRelationItemWriterFactory.create(
                EntityType.MASTER, runId, chunkSize, resumed))
        .faultTolerant()
        .retryLimit(10)
        .retry(PessimisticLockingFailureException.class)
        .listener(stringNormalizingItemReadListener)
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .listener(
            new EntityProgressStepExecutionListener(
                importProgressStore, EntityType.MASTER, runId, chunkSize))
        .taskExecutor(taskExecutor)
        .build();
  }

  @Bean
  @JobScope
  public Step masterGenreStyleInsertionStep() {
    return new StepBuilder(MASTER_GENRE_STYLE_INSERTION_STEP, jobRepository)
        .tasklet(genreStyleInsertionTasklet, transactionManager)
        .build();
  }
}
