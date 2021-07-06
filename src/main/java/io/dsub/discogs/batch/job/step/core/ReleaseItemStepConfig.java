package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.decider.MasterMainReleaseStepJobExecutionDecider;
import io.dsub.discogs.batch.job.listener.CacheInversionStepExecutionListener;
import io.dsub.discogs.batch.job.listener.IdCachingItemProcessListener;
import io.dsub.discogs.batch.job.listener.ItemCountingItemProcessListener;
import io.dsub.discogs.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogs.batch.job.listener.StringNormalizingItemReadListener;
import io.dsub.discogs.batch.job.step.AbstractStepConfig;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.job.tasklet.GenreStyleInsertionTasklet;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.jooq.tables.records.MasterRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemRecord;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.UpdatableRecord;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

  private final SynchronizedItemStreamReader<ReleaseItemSubItemsXML>
      releaseItemSubItemsStreamReader;
  private final SynchronizedItemStreamReader<ReleaseItemXML> releaseItemStreamReader;
  private final SynchronizedItemStreamReader<MasterMainReleaseXML> masterMainReleaseStreamReader;

  private final ItemProcessor<ReleaseItemSubItemsXML, Collection<UpdatableRecord<?>>>
      releaseItemSubItemsProcessor;
  private final ItemProcessor<ReleaseItemXML, ReleaseItemRecord> releaseItemCoreProcessor;
  private final ItemProcessor<MasterMainReleaseXML, MasterRecord> masterMainReleaseItemProcessor;

  private final ItemWriter<UpdatableRecord<?>> entityItemWriter;
  private final ItemWriter<Collection<UpdatableRecord<?>>> collectionItemWriter;
  private final ItemWriter<MasterRecord> postgresJooqMasterMainReleaseItemWriter;

  private final DiscogsDump releaseItemDump;

  private final StepBuilderFactory sbf;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final FileUtil fileUtil;
  private final GenreStyleInsertionTasklet genreStyleInsertionTasklet;

  private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
  private final StringNormalizingItemReadListener stringNormalizingItemReadListener;
  private final ItemCountingItemProcessListener itemCountingItemProcessListener;
  private final IdCachingItemProcessListener idCachingItemProcessListener;
  private final CacheInversionStepExecutionListener cacheInversionStepExecutionListener;
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
            .end()
            .from(releaseFileFetchStep())
            .on(ANY)
            .to(releaseItemCoreInsertionStep(chunkSize))

            // from core insertion
            .from(releaseItemCoreInsertionStep(chunkSize))
            .on(FAILED)
            .end()
            .from(releaseItemCoreInsertionStep(chunkSize))
            .on(ANY)
            .to(releaseGenreStyleInsertionStep())

            // from genre style insertion step
            .from(releaseGenreStyleInsertionStep())
            .on(FAILED)
            .end()
            .from(releaseGenreStyleInsertionStep())
            .on(ANY)
            .to(releaseItemSubItemsInsertionStep(chunkSize))

            // from sub items insertion
            .from(releaseItemSubItemsInsertionStep(chunkSize))
            .on(FAILED)
            .end()
            .from(releaseItemSubItemsInsertionStep(chunkSize))
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
            .on(ANY)
            .end()
            // conclude
            .build();
    // @formatter:on

    FlowStep artistFlowStep = new FlowStep();
    artistFlowStep.setJobRepository(jobRepository);
    artistFlowStep.setName(RELEASE_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step releaseItemCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(RELEASE_ITEM_CORE_INSERTION_STEP)
        .<ReleaseItemXML, UpdatableRecord<?>>chunk(chunkSize)
        .reader(releaseItemStreamReader)
        .processor(releaseItemCoreProcessor)
        .writer(entityItemWriter)
        .faultTolerant()
        .retryLimit(100)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(stopWatchStepExecutionListener)
        .listener(stringNormalizingItemReadListener)
        .listener(itemCountingItemProcessListener)
        .listener(idCachingItemProcessListener)
        .listener(cacheInversionStepExecutionListener)
        .taskExecutor(taskExecutor)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }

  @Bean
  @JobScope
  public Step releaseItemSubItemsInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(RELEASE_ITEM_SUB_ITEMS_INSERTION_STEP)
        .<ReleaseItemSubItemsXML, Collection<UpdatableRecord<?>>>chunk(
            Integer.divideUnsigned(chunkSize, 2)) // due to memory consumptions
        .reader(releaseItemSubItemsStreamReader)
        .processor(releaseItemSubItemsProcessor)
        .writer(collectionItemWriter)
        .faultTolerant()
        .retryLimit(100)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(stringNormalizingItemReadListener)
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .taskExecutor(taskExecutor)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }

  @Bean
  @JobScope
  public Step releaseFileFetchStep() throws DumpNotFoundException {
    return sbf.get(RELEASE_FILE_FETCH_STEP)
        .tasklet(new FileFetchTasklet(releaseItemDump, fileUtil))
        .build();
  }

  @Bean
  @JobScope
  public Step masterMainReleaseUpdateStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(MASTER_MAIN_RELEASE_UPDATE_STEP)
        .<MasterMainReleaseXML, MasterRecord>chunk(chunkSize)
        .reader(masterMainReleaseStreamReader)
        .processor(masterMainReleaseItemProcessor)
        .writer(postgresJooqMasterMainReleaseItemWriter)
        .listener(stopWatchStepExecutionListener)
        .listener(itemCountingItemProcessListener)
        .taskExecutor(taskExecutor)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }

  @Bean
  @JobScope
  public Step releaseGenreStyleInsertionStep() {
    return sbf.get(RELEASE_GENRE_STYLE_INSERTION_STEP).tasklet(genreStyleInsertionTasklet).build();
  }
}
