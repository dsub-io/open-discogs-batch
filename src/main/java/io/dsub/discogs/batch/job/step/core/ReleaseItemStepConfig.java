package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DiscogsDumpVerifier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.BatchRetryPolicy;
import io.dsub.discogs.batch.job.listener.EntityProgressStepExecutionListener;
import io.dsub.discogs.batch.job.listener.ItemCountingItemProcessListener;
import io.dsub.discogs.batch.job.listener.NestedStepFailurePropagatingListener;
import io.dsub.discogs.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogs.batch.job.processor.ReleaseRootMutation;
import io.dsub.discogs.batch.job.processor.ResumeAwareSourceChunkItemProcessor;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import io.dsub.discogs.batch.job.reader.SourceChunkItemStreamReader;
import io.dsub.discogs.batch.job.step.AbstractStepConfig;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.job.tasklet.MasterMainReleaseReconciliationTasklet;
import io.dsub.discogs.batch.job.writer.DurableReleaseItemWriterFactory;
import io.dsub.discogs.batch.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReleaseItemStepConfig extends AbstractStepConfig {

  public static final String RELEASE_STEP_FLOW = "release item step flow";
  public static final String RELEASE_FLOW_STEP = "release item flow step";
  public static final String RELEASE_ITEM_SUB_ITEMS_INSERTION_STEP =
      "release item sub items insertion step";
  public static final String RELEASE_MAIN_RELEASE_RECONCILIATION_STEP =
      "release main release reconciliation step";
  public static final String RELEASE_FILE_FETCH_STEP = "release item file fetch step";

  private final SourceChunkItemStreamReader<ReleaseItemSubItemsXML>
      releaseItemSubItemsStreamReader;

  private final ItemProcessor<
          SourceChunk<ReleaseItemSubItemsXML>, ProcessedChunk<ReleaseRootMutation>>
      releaseItemSubItemsProcessor;

  private final DurableReleaseItemWriterFactory durableReleaseItemWriterFactory;
  private final MasterMainReleaseReconciliationTasklet mainReleaseReconciliationTasklet;
  private final ImportProgressStore importProgressStore;

  private final DiscogsDump releaseItemDump;

  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final FileUtil fileUtil;
  private final DiscogsDumpVerifier dumpVerifier;

  private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
  private final ItemCountingItemProcessListener itemCountingItemProcessListener;

  @Bean
  @JobScope
  public Step releaseStep(
      @Value(CHUNK) Integer chunkSize,
      @Value(RUN_ID) Long runId,
      @Value(RESUMED) Boolean resumed)
      throws InvalidArgumentException, DumpNotFoundException {
    // @formatter:off
    Flow releaseStepFlow =
        new FlowBuilder<SimpleFlow>(RELEASE_STEP_FLOW)

            // from execution decider
            .from(executionDecider(
                RELEASE, EntityType.RELEASE, importProgressStore, runId, chunkSize, resumed))
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
            .to(releaseItemSubItemsInsertionStep(chunkSize, null, null))

            // from sub items insertion
            .from(releaseItemSubItemsInsertionStep(chunkSize, null, null))
            .on(FAILED)
            .fail()
            .from(releaseItemSubItemsInsertionStep(chunkSize, null, null))
            .on(ANY)
            .to(releaseMainReleaseReconciliationStep(null, null, null))

            // from main release reconciliation
            .from(releaseMainReleaseReconciliationStep(null, null, null))
            .on(FAILED)
            .fail()
            .from(releaseMainReleaseReconciliationStep(null, null, null))
            .on(ANY)
            .end()
            // conclude
            .build();
    // @formatter:on

    FlowStep releaseFlowStep = new FlowStep(jobRepository);
    releaseFlowStep.setName(RELEASE_FLOW_STEP);
    releaseFlowStep.setStartLimit(Integer.MAX_VALUE);
    releaseFlowStep.setFlow(releaseStepFlow);
    releaseFlowStep.registerStepExecutionListener(new NestedStepFailurePropagatingListener());
    return releaseFlowStep;
  }

  @Bean
  @JobScope
  public Step releaseItemSubItemsInsertionStep(
      @Value(CHUNK) Integer chunkSize,
      @Value(RUN_ID) Long runId,
      @Value(RESUMED) Boolean resumed) {
    return new StepBuilder(RELEASE_ITEM_SUB_ITEMS_INSERTION_STEP, jobRepository)
        .<SourceChunk<ReleaseItemSubItemsXML>, ProcessedChunk<ReleaseRootMutation>>chunk(
            TRACKED_CHUNKS_PER_TRANSACTION)
        .transactionManager(transactionManager)
        .reader(releaseItemSubItemsStreamReader)
        .processor(
            new ResumeAwareSourceChunkItemProcessor<>(
                releaseItemSubItemsProcessor,
                importProgressStore.loadCompletedChunks(runId, EntityType.RELEASE, resumed)))
        .writer(
            durableReleaseItemWriterFactory.create(runId, chunkSize, resumed))
        .faultTolerant()
        .retryPolicy(BatchRetryPolicy.lockContention())
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .listener(
            new EntityProgressStepExecutionListener(
                importProgressStore,
                EntityType.RELEASE,
                runId,
                chunkSize,
                resumed,
                EntityProgressStepExecutionListener.CompletionPolicy.DEFER))
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
  public Step releaseMainReleaseReconciliationStep(
      @Value(CHUNK) Integer chunkSize,
      @Value(RUN_ID) Long runId,
      @Value(RESUMED) Boolean resumed) {
    StepExecutionListener completionListener =
        new EntityProgressStepExecutionListener(
            importProgressStore, EntityType.RELEASE, runId, chunkSize, resumed);
    return new StepBuilder(RELEASE_MAIN_RELEASE_RECONCILIATION_STEP, jobRepository)
        .tasklet(mainReleaseReconciliationTasklet, transactionManager)
        .listener(completionListener)
        .build();
  }

}
