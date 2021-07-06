package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
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
  private final SynchronizedItemStreamReader<MasterSubItemsXML> masterSubItemsStreamReader;
  private final ItemProcessor<MasterXML, MasterRecord> masterCoreProcessor;
  private final ItemProcessor<MasterSubItemsXML, Collection<UpdatableRecord<?>>>
      masterSubItemsProcessor;
  private final ItemWriter<Collection<UpdatableRecord<?>>> collectionItemWriter;
  private final ItemWriter<UpdatableRecord<?>> entityItemWriter;
  private final DiscogsDump masterDump;

  private final StepBuilderFactory sbf;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final FileUtil fileUtil;
  private final GenreStyleInsertionTasklet genreStyleInsertionTasklet;

  private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
  private final CacheInversionStepExecutionListener cacheInversionStepExecutionListener;
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
            .end()
            .from(masterFileFetchStep())
            .on(ANY)
            .to(masterCoreInsertionStep(chunkSize))

            // from core insertion
            .from(masterCoreInsertionStep(chunkSize))
            .on(FAILED)
            .end()
            .from(masterCoreInsertionStep(chunkSize))
            .on(ANY)
            .to(masterGenreStyleInsertionStep())

            // from master genre style insertion step
            .from(masterGenreStyleInsertionStep())
            .on(FAILED)
            .end()
            .from(masterGenreStyleInsertionStep())
            .on(ANY)
            .to(masterSubItemsInsertionStep(chunkSize))

            // from sub items insertion
            .from(masterSubItemsInsertionStep(chunkSize))
            .on(ANY)
            .end()

            // conclude
            .build();
    // @formatter:on

    FlowStep artistFlowStep = new FlowStep();
    artistFlowStep.setJobRepository(jobRepository);
    artistFlowStep.setName(MASTER_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step masterFileFetchStep() throws DumpNotFoundException {
    return sbf.get(MASTER_FILE_FETCH_STEP)
        .tasklet(new FileFetchTasklet(masterDump, fileUtil))
        .build();
  }

  @Bean
  @JobScope
  public Step masterCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(MASTER_CORE_INSERTION_STEP)
        .<MasterXML, UpdatableRecord<?>>chunk(chunkSize)
        .reader(masterStreamReader)
        .processor(masterCoreProcessor)
        .writer(entityItemWriter)
        .faultTolerant()
        .retryLimit(10)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(stopWatchStepExecutionListener)
        .listener(stringNormalizingItemReadListener)
        .listener(idCachingItemProcessListener)
        .listener(itemCountingItemProcessListener)
        .listener(cacheInversionStepExecutionListener)
        .taskExecutor(taskExecutor)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }

  @Bean
  @JobScope
  public Step masterSubItemsInsertionStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(MASTER_SUB_ITEMS_INSERTION_STEP)
        .<MasterSubItemsXML, Collection<UpdatableRecord<?>>>chunk(chunkSize)
        .reader(masterSubItemsStreamReader)
        .processor(masterSubItemsProcessor)
        .writer(collectionItemWriter)
        .faultTolerant()
        .retryLimit(10)
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
  public Step masterGenreStyleInsertionStep() {
    return sbf.get(MASTER_GENRE_STYLE_INSERTION_STEP).tasklet(genreStyleInsertionTasklet).build();
  }
}
