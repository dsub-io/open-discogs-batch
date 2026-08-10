package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
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
import io.dsub.opendiscogs.jooq.tables.records.LabelRecord;
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
public class LabelStepConfig extends AbstractStepConfig {

  public static final String LABEL_STEP_FLOW = "label step flow";
  public static final String LABEL_FLOW_STEP = "label flow step";
  public static final String LABEL_CORE_INSERTION_STEP = "label core insertion step";
  public static final String LABEL_SUB_ITEMS_INSERTION_STEP = "label sub items insertion step";
  public static final String LABEL_FILE_FETCH_STEP = "label file fetch step";
  public static final String LABEL_FILE_CLEAR_STEP = "label file clear step";

  private final SynchronizedItemStreamReader<LabelXML> labelStreamReader;
  private final SourceChunkItemStreamReader<LabelSubItemsXML> labelSubItemsStreamReader;

  private final ItemProcessor<LabelXML, LabelRecord> labelCoreProcessor;
  private final ItemProcessor<SourceChunk<LabelSubItemsXML>, ProcessedChunk<RelationSet>>
      labelSubItemsProcessor;
  private final DurableRelationItemWriterFactory durableRelationItemWriterFactory;
  private final ImportProgressStore importProgressStore;
  private final ItemWriter<UpdatableRecord<?>> entityItemWriter;
  private final DiscogsDump labelDump;

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
  public Step labelStep() throws InvalidArgumentException, DumpNotFoundException {

    // @formatter:off
    Flow labelStepFlow =
        new FlowBuilder<SimpleFlow>(LABEL_STEP_FLOW)

            // from execution decider
            .from(executionDecider(LABEL))
            .on(SKIPPED)
            .end()
            .on(ANY)
            .to(labelFileFetchStep())

            // from fetch
            .from(labelFileFetchStep())
            .on(FAILED)
            .fail()
            .from(labelFileFetchStep())
            .on(ANY)
            .to(labelCoreInsertionStep(null))

            // from core item insertion
            .from(labelCoreInsertionStep(null))
            .on(FAILED)
            .fail()
            .from(labelCoreInsertionStep(null))
            .on(ANY)
            .to(labelSubItemsInsertionStep(null, null, null))

            // from sub items insertion
            .from(labelSubItemsInsertionStep(null, null, null))
            .on(FAILED)
            .fail()
            .from(labelSubItemsInsertionStep(null, null, null))
            .on(ANY)
            .end()

            // conclude
            .build();
    // @formatter:on

    FlowStep labelFlowStep = new FlowStep(jobRepository);
    labelFlowStep.setName(LABEL_FLOW_STEP);
    labelFlowStep.setStartLimit(Integer.MAX_VALUE);
    labelFlowStep.setFlow(labelStepFlow);
    labelFlowStep.registerStepExecutionListener(new NestedStepFailurePropagatingListener());
    return labelFlowStep;
  }

  @Bean
  @JobScope
  public Step labelCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return new StepBuilder(LABEL_CORE_INSERTION_STEP, jobRepository)
        .<LabelXML, UpdatableRecord<?>>chunk(chunkSize)
        .transactionManager(transactionManager)
        .reader(labelStreamReader)
        .processor(labelCoreProcessor)
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
  public Step labelSubItemsInsertionStep(
      @Value(CHUNK) Integer chunkSize,
      @Value(RUN_ID) Long runId,
      @Value(RESUMED) Boolean resumed) {
    return new StepBuilder(LABEL_SUB_ITEMS_INSERTION_STEP, jobRepository)
        .<SourceChunk<LabelSubItemsXML>, ProcessedChunk<RelationSet>>chunk(
            TRACKED_CHUNKS_PER_TRANSACTION)
        .transactionManager(transactionManager)
        .reader(labelSubItemsStreamReader)
        .processor(labelSubItemsProcessor)
        .writer(
            durableRelationItemWriterFactory.create(
                EntityType.LABEL, runId, chunkSize, resumed))
        .faultTolerant()
        .retryLimit(10)
        .retry(PessimisticLockingFailureException.class)
        .listener(stringNormalizingItemReadListener)
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .listener(
            new EntityProgressStepExecutionListener(
                importProgressStore, EntityType.LABEL, runId, chunkSize))
        .taskExecutor(taskExecutor)
        .build();
  }

  @Bean
  @JobScope
  public Step labelFileFetchStep() throws DumpNotFoundException {
    return new StepBuilder(LABEL_FILE_FETCH_STEP, jobRepository)
        .tasklet(
            new FileFetchTasklet(labelDump, fileUtil, dumpVerifier), transactionManager)
        .build();
  }
}
