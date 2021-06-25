package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.label.LabelCommand;
import io.dsub.discogs.batch.domain.label.LabelSubItemsCommand;
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
public class LabelStepConfig extends AbstractStepConfig {

    public static final String LABEL_STEP_FLOW = "label step flow";
    public static final String LABEL_FLOW_STEP = "label flow step";
    public static final String LABEL_CORE_INSERTION_STEP = "label core insertion step";
    public static final String LABEL_SUB_ITEMS_INSERTION_STEP = "label sub items insertion step";
    public static final String LABEL_FILE_FETCH_STEP = "label file fetch step";
    public static final String LABEL_FILE_CLEAR_STEP = "label file clear step";

    private final SynchronizedItemStreamReader<LabelCommand> labelStreamReader;
    private final SynchronizedItemStreamReader<LabelSubItemsCommand> labelSubItemsStreamReader;

    private final ItemProcessor<LabelCommand, BaseEntity> labelCoreProcessor;
    private final ItemProcessor<LabelSubItemsCommand, Collection<BaseEntity>> labelSubItemsProcessor;
    private final ItemWriter<Collection<BaseEntity>> collectionItemWriter;
    private final ItemWriter<BaseEntity> entityItemWriter;
    private final DiscogsDump labelDump;

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
    public Step labelStep() throws InvalidArgumentException, DumpNotFoundException {

        // @formatter:off
        Flow labelStepFlow =
                new FlowBuilder<SimpleFlow>(LABEL_STEP_FLOW)

                        // from execution decider
                        .from(executionDecider(LABEL))
                                .on(SKIPPED).end()
                        .from(executionDecider(LABEL))
                                .on(ANY).to(labelFileFetchStep())

                        // from fetch
                        .from(labelFileFetchStep())
                                .on(FAILED).to(labelFileClearStep())
                        .from(labelFileFetchStep())
                                .on(ANY).to(labelCoreInsertionStep(null))

                        // from core item insertion
                        .from(labelCoreInsertionStep(null))
                                .on(FAILED).to(labelFileFetchStep())
                        .from(labelCoreInsertionStep(null))
                                .on(ANY).to(labelSubItemsInsertionStep(null))

                        // from sub items insertion
                        .from(labelSubItemsInsertionStep(null))
                                .on(ANY).to(labelFileClearStep())

                        // from file clear
                        .from(labelFileClearStep())
                                .on(ANY).end()

                        // conclude
                        .build();
        // @formatter:on

        FlowStep artistFlowStep = new FlowStep();
        artistFlowStep.setJobRepository(jobRepository);
        artistFlowStep.setName(LABEL_FLOW_STEP);
        artistFlowStep.setStartLimit(Integer.MAX_VALUE);
        artistFlowStep.setFlow(labelStepFlow);
        return artistFlowStep;
    }

    @Bean
    @JobScope
    public Step labelCoreInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(LABEL_CORE_INSERTION_STEP)
                .<LabelCommand, BaseEntity>chunk(chunkSize)
                .reader(labelStreamReader)
                .processor(labelCoreProcessor)
                .writer(entityItemWriter)
                .faultTolerant()
                .retryLimit(10)
                .retry(DeadlockLoserDataAccessException.class)
                .listener(stopWatchStepExecutionListener)
                .listener(stringNormalizingItemReadListener)
                .listener(idCachingItemProcessListener)
                .listener(itemCountingItemProcessListener)
                .listener(cacheInversionStepExecutionListener)
                .transactionManager(transactionManager)
                .taskExecutor(taskExecutor)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .build();
    }

    @Bean
    @JobScope
    public Step labelSubItemsInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(LABEL_SUB_ITEMS_INSERTION_STEP)
                .<LabelSubItemsCommand, Collection<BaseEntity>>chunk(chunkSize)
                .reader(labelSubItemsStreamReader)
                .processor(labelSubItemsProcessor)
                .writer(collectionItemWriter)
                .faultTolerant()
                .retryLimit(10)
                .retry(DeadlockLoserDataAccessException.class)
                .listener(stringNormalizingItemReadListener)
                .listener(stopWatchStepExecutionListener)
                .listener(itemCountingItemProcessListener)
                .transactionManager(transactionManager)
                .taskExecutor(taskExecutor)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .build();
    }

    @Bean
    @JobScope
    public Step labelFileFetchStep() throws DumpNotFoundException {
        return sbf.get(LABEL_FILE_FETCH_STEP)
                .tasklet(new FileFetchTasklet(labelDump, fileUtil))
                .build();
    }

    @Bean
    @JobScope
    public Step labelFileClearStep() {
        return sbf.get(LABEL_FILE_CLEAR_STEP).tasklet(new FileClearTasklet(fileUtil)).build();
    }
}
