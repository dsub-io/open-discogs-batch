package io.dsub.discogs.batch.job.step.core;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.listener.*;
import io.dsub.discogs.batch.job.step.AbstractStepConfig;
import io.dsub.discogs.batch.job.tasklet.FileClearTasklet;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.common.jooq.postgres.tables.records.ArtistRecord;
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

import java.util.Collection;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistStepConfig extends AbstractStepConfig {

    public static final String ARTIST_STEP_FLOW = "artist step flow";
    public static final String ARTIST_FLOW_STEP = "artist flow step";
    public static final String ARTIST_CORE_INSERTION_STEP = "artist core insertion step";
    public static final String ARTIST_SUB_ITEMS_INSERTION_STEP = "artist sub items insertion step";
    public static final String ARTIST_FILE_FETCH_STEP = "artist file fetch step";
    public static final String ARTIST_FILE_CLEAR_STEP = "artist file clear step";

    private final SynchronizedItemStreamReader<ArtistXML> artistStreamReader;
    private final SynchronizedItemStreamReader<ArtistSubItemsXML> artistSubItemsStreamReader;
    private final ItemProcessor<ArtistSubItemsXML, Collection<UpdatableRecord<?>>> artistSubItemsProcessor;
    private final ItemProcessor<ArtistXML, ArtistRecord> artistCoreProcessor;
    private final ItemWriter<UpdatableRecord<?>> entityItemWriter;
    private final ItemWriter<Collection<UpdatableRecord<?>>>CollectionItemWriter;
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
                                .on(FAILED).end()
                        .from(artistFileFetchStep())
                                .on(ANY).to(artistCoreInsertionStep(null))

                        // from core insert
                        .from(artistCoreInsertionStep(null))
                                .on(FAILED).end()
                        .from(artistCoreInsertionStep(null))
                                .on(ANY).to(artistSubItemsInsertionStep(null))

                        // from sub items insert
                        .from(artistSubItemsInsertionStep(null))
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
                .<ArtistXML, UpdatableRecord<?>>chunk(chunkSize)
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
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    @JobScope
    public Step artistSubItemsInsertionStep(@Value(CHUNK) Integer chunkSize) {
        return sbf.get(ARTIST_SUB_ITEMS_INSERTION_STEP)
                .<ArtistSubItemsXML, Collection<UpdatableRecord<?>>>chunk(chunkSize)
                .reader(artistSubItemsStreamReader)
                .processor(artistSubItemsProcessor)
                .writer(CollectionItemWriter)
                .faultTolerant()
                .retryLimit(100)
                .retry(DeadlockLoserDataAccessException.class)
                .listener(stringNormalizingItemReadListener)
                .listener(stopWatchStepExecutionListener)
                .listener(itemCountingItemProcessListener)
                .taskExecutor(taskExecutor)
                .throttleLimit(taskExecutor.getMaxPoolSize())
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    @JobScope
    public Step artistFileFetchStep() throws DumpNotFoundException {
        return sbf.get(ARTIST_FILE_FETCH_STEP)
                .tasklet(new FileFetchTasklet(artistDump, fileUtil))
                .build();
    }
}
