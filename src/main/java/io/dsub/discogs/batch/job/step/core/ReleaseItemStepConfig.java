package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.release.ReleaseItemCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsCommand;
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
public class ReleaseItemStepConfig extends AbstractStepConfig {

    private static final String RELEASE_STEP_FLOW = "release item step flow";
    private static final String RELEASE_FLOW_STEP = "release item flow step";
    private static final String RELEASE_ITEM_CORE_INSERTION_STEP = "release item core insertion step";
    private static final String RELEASE_ITEM_SUB_ITEMS_INSERTION_STEP = "release item sub items insertion step";
    private static final String RELEASE_FILE_FETCH_STEP = "release item file fetch step";
    private static final String RELEASE_FILE_CLEAR_STEP = "release item file clear step";

    private final SynchronizedItemStreamReader<ReleaseItemSubItemsCommand> releaseItemSubItemsStreamReader;
    private final SynchronizedItemStreamReader<ReleaseItemCommand> releaseItemStreamReader;

    private final ItemProcessor<ReleaseItemSubItemsCommand, Collection<BaseEntity>> releaseItemSubItemsProcessor;
    private final ItemProcessor<ReleaseItemCommand, BaseEntity> releaseItemCoreProcessor;

    private final ItemWriter<BaseEntity> entityItemWriter;
    private final ItemWriter<Collection<BaseEntity>> collectionItemWriter;

    private final DiscogsDump releaseItemDump;

    private final StepBuilderFactory sbf;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final JobRepository jobRepository;
    private final FileUtil fileUtil;

    private final StopWatchStepExecutionListener stopWatchStepExecutionListener;
    private final StringNormalizingItemReadListener stringNormalizingItemReadListener;
    private final ItemCountingItemProcessListener itemCountingItemProcessListener;

    private PlatformTransactionManager transactionManager;

    @Autowired
    public void setStepTransactionManager(@Qualifier(value = "stepTransactionManager") PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Bean
    @JobScope
    public Step releaseStep(@Value(CHUNK) Integer chunkSize) throws InvalidArgumentException, DumpNotFoundException {

        // @formatter:off
        Flow artistStepFlow =
                new FlowBuilder<SimpleFlow>(RELEASE_STEP_FLOW)

                        // from execution decider
                        .from(executionDecider(RELEASE))
                                .on(SKIPPED).end()
                        .from(executionDecider(RELEASE))
                                .on(ANY).to(releaseFileFetchStep())

                        // from fetch
                        .from(releaseFileFetchStep())
                                .on(FAILED).to(releaseFileClearStep())
                        .from(releaseFileFetchStep())
                                .on(ANY).to(releaseItemCoreInsertionStep(chunkSize))

                        // from core insertion
                        .from(releaseItemCoreInsertionStep(chunkSize))
                                .on(FAILED).to(releaseFileClearStep())
                        .from(releaseItemCoreInsertionStep(chunkSize))
                                .on(ANY).to(releaseFileClearStep())

                        // from sub items insertion
                        .from(releaseItemSubItemsInsertionStep(chunkSize))
                                .on(ANY).to(releaseFileClearStep())

                        // from file clear
                        .from(releaseFileClearStep())
                                .on(ANY).end()

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
                .<ReleaseItemCommand, BaseEntity>chunk(chunkSize)
                .reader(releaseItemStreamReader)
                .processor(releaseItemCoreProcessor)
                .writer(entityItemWriter)
                .faultTolerant()
                .retryLimit(100)
                .retry(DeadlockLoserDataAccessException.class)
                .listener(stopWatchStepExecutionListener)
                .listener(stringNormalizingItemReadListener)
                .listener(itemCountingItemProcessListener)
                .taskExecutor(taskExecutor)
                .transactionManager(transactionManager)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .build();
    }

    @Bean
    @JobScope
    public Step releaseItemSubItemsInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(RELEASE_ITEM_SUB_ITEMS_INSERTION_STEP)
                .<ReleaseItemSubItemsCommand, Collection<BaseEntity>>chunk(chunkSize)
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
                .transactionManager(transactionManager)
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
    public Step releaseFileClearStep() {
        return sbf.get(RELEASE_FILE_CLEAR_STEP).tasklet(new FileClearTasklet(fileUtil)).build();
    }
}
