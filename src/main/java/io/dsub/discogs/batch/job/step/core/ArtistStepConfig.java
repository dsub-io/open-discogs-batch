package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.artist.ArtistCommand;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.listener.*;
import io.dsub.discogs.batch.job.step.AbstractStepConfig;
import io.dsub.discogs.batch.job.tasklet.FileClearTasklet;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.common.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import java.util.Collection;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistStepConfig extends AbstractStepConfig implements InitializingBean {

    public static final String ARTIST_STEP_FLOW = "artist step flow";
    public static final String ARTIST_FLOW_STEP = "artist flow step";
    public static final String ARTIST_CORE_INSERTION_STEP = "artist core insertion step";
    public static final String ARTIST_SUB_ITEMS_INSERTION_STEP = "artist sub items insertion step";
    public static final String ARTIST_FILE_FETCH_STEP = "artist file fetch step";
    public static final String ARTIST_FILE_CLEAR_STEP = "artist file clear step";

    private final SynchronizedItemStreamReader<ArtistCommand> artistStreamReader;
    private final SynchronizedItemStreamReader<ArtistSubItemsCommand> artistSubItemsStreamReader;
    private final ItemProcessor<ArtistSubItemsCommand, Collection<BaseEntity>> artistSubItemsProcessor;
    private final ItemProcessor<ArtistCommand, BaseEntity> artistCoreProcessor;
    private final ItemWriter<BaseEntity> entityItemWriter;
    private final ItemWriter<Collection<BaseEntity>> baseEntityCollectionItemWriter;
    private final DiscogsDump artistDump;
    private final StepBuilderFactory sbf;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final JobRepository jobRepository;
    private final FileUtil fileUtil;

    private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
    private final CacheInversionStepExecutionListener cacheInversionStepExecutionListener;
    private final StringNormalizingItemReadListener stringNormalizingItemReadListener;
    private final IdCachingItemProcessListener idCachingItemProcessListener;
    private final ItemCountingItemProcessListener itemCountingItemProcessListener;

    private PlatformTransactionManager transactionManager;

    @Autowired
    public void setTransactionManager(@Qualifier(value = "stepTransactionManager") PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Bean
    @JobScope
    public Step artistStep() throws InvalidArgumentException, DumpNotFoundException {

        // @formatter:off
        Flow artistStepFlow =
                new FlowBuilder<SimpleFlow>(ARTIST_STEP_FLOW)

                        // execution decider
                        .from(executionDecider(ARTIST))
                                .on(SKIPPED).end()
                                .on(ANY).to(artistFileFetchStep())

                        // from fetch
                        .from(artistFileFetchStep())
                                .on(FAILED).to(artistFileClearStep())
                        .from(artistFileFetchStep())
                                .on(ANY).to(artistCoreInsertionStep(null))

                        // from core insert
                        .from(artistCoreInsertionStep(null))
                                .on(FAILED).to(artistFileClearStep())
                        .from(artistCoreInsertionStep(null))
                                .on(ANY).to(artistSubItemsInsertionStep(null))

                        // from sub items insert
                        .from(artistSubItemsInsertionStep(null))
                                .on(ANY).to(artistFileClearStep())

                        // from file clear
                        .from(artistFileClearStep())
                                .on(ANY).end()

                        // conclude
                        .build();
        // @formatter:on

        FlowStep artistFlowStep = new FlowStep();
        artistFlowStep.setJobRepository(jobRepository);
        artistFlowStep.setName(ARTIST_FLOW_STEP);
        artistFlowStep.setStartLimit(Integer.MAX_VALUE);
        artistFlowStep.setFlow(artistStepFlow);
        return artistFlowStep;
    }

    @Bean
    @JobScope
    public Step artistCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(ARTIST_CORE_INSERTION_STEP)
                .<ArtistCommand, BaseEntity>chunk(chunkSize)
                .reader(artistStreamReader)
                .processor(artistCoreProcessor)
                .writer(entityItemWriter)
                .faultTolerant()
                .retryLimit(100)
                .retry(DeadlockLoserDataAccessException.class)
                .listener(stopWatchStepExecutionListener)
                .listener(stringNormalizingItemReadListener)
                .listener(idCachingItemProcessListener)
                .listener(itemCountingItemProcessListener)
                .listener(cacheInversionStepExecutionListener)
                .taskExecutor(taskExecutor)
                .transactionManager(transactionManager)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .build();
    }

    @Bean
    @JobScope
    public Step artistSubItemsInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(ARTIST_SUB_ITEMS_INSERTION_STEP)
                .<ArtistSubItemsCommand, Collection<BaseEntity>>chunk(chunkSize)
                .reader(artistSubItemsStreamReader)
                .processor(artistSubItemsProcessor)
                .writer(baseEntityCollectionItemWriter)
                .faultTolerant()
                .retryLimit(100)
                .retry(DeadlockLoserDataAccessException.class)
                .listener(stringNormalizingItemReadListener)
                .listener(stopWatchStepExecutionListener)
                .listener(itemCountingItemProcessListener)
                .taskExecutor(taskExecutor)
                .transactionManager(transactionManager)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .build();
    }

    @Bean
    @JobScope
    public Step artistFileFetchStep() throws DumpNotFoundException {
        return sbf.get(ARTIST_FILE_FETCH_STEP)
                .tasklet(new FileFetchTasklet(artistDump, fileUtil))
                .build();
    }

    @Bean
    @JobScope
    public Step artistFileClearStep() {
        return sbf.get(ARTIST_FILE_CLEAR_STEP).tasklet(new FileClearTasklet(fileUtil)).build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(transactionManager,"stepTransactionManager cannot be null");
    }
}
