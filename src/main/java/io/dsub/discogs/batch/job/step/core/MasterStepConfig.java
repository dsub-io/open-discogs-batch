package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.master.MasterCommand;
import io.dsub.discogs.batch.domain.master.MasterSubItemsCommand;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;

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

    private final SynchronizedItemStreamReader<MasterCommand> masterStreamReader;
    private final SynchronizedItemStreamReader<MasterSubItemsCommand> masterSubItemsStreamReader;
    private final ItemProcessor<MasterCommand, BaseEntity> masterCoreProcessor;
    private final ItemProcessor<MasterSubItemsCommand, Collection<BaseEntity>> masterSubItemsProcessor;
    private final ItemWriter<Collection<BaseEntity>> collectionItemWriter;
    private final ItemWriter<BaseEntity> entityItemWriter;
    private final DiscogsDump masterDump;

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
    public void setStepTransactionManager(@Qualifier(value = "stepTransactionManager") PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Bean
    @JobScope
    public Step masterStep(@Value(CHUNK) Integer chunkSize) throws InvalidArgumentException, DumpNotFoundException {

        // @formatter:off
        Flow artistStepFlow =
                new FlowBuilder<SimpleFlow>(MASTER_STEP_FLOW)

                        // from execution decider
                        .from(executionDecider(MASTER))
                                .on(SKIPPED).end()
                        .from(executionDecider(MASTER))
                                .on(ANY).to(masterFileFetchStep())

                        // from fetch
                        .from(masterFileFetchStep())
                                .on(FAILED)
                                .to(masterFileClearStep())
                        .from(masterFileFetchStep())
                                .on(ANY)
                                .to(masterCoreInsertionStep(chunkSize))

                        // from core insertion
                        .from(masterCoreInsertionStep(chunkSize))
                                .on(FAILED).to(masterFileClearStep())
                        .from(masterCoreInsertionStep(chunkSize))
                                .on(ANY).to(masterSubItemsInsertionStep(chunkSize))

                        // from sub items insertion
                        .from(masterSubItemsInsertionStep(chunkSize))
                                .on(ANY).to(masterFileClearStep())

                        // from file clear
                        .from(masterFileClearStep())
                                .on(ANY).end()

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
    public Step masterFileClearStep() {
        return sbf.get(MASTER_FILE_CLEAR_STEP).tasklet(new FileClearTasklet(fileUtil)).build();
    }

    @Bean
    @JobScope
    public Step masterCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(MASTER_CORE_INSERTION_STEP)
                .<MasterCommand, BaseEntity>chunk(chunkSize)
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
                .transactionManager(transactionManager)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .build();
    }

    @Bean
    @JobScope
    public Step masterSubItemsInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(MASTER_SUB_ITEMS_INSERTION_STEP)
                .<MasterSubItemsCommand, Collection<BaseEntity>>chunk(chunkSize)
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
                .transactionManager(transactionManager)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .build();
    }
}
