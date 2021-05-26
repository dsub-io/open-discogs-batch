package io.dsub.discogsdata.batch.job.step;

import io.dsub.discogsdata.batch.BatchCommand;
import io.dsub.discogsdata.batch.domain.artist.ArtistXML;
import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.service.DiscogsDumpService;
import io.dsub.discogsdata.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogsdata.batch.job.listener.StringFieldNormalizingItemReadListener;
import io.dsub.discogsdata.batch.job.processor.ArtistSubItemsProcessor;
import io.dsub.discogsdata.batch.job.reader.DumpItemReaderBuilder;
import io.dsub.discogsdata.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import java.net.URL;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ArtistStepConfig {

  private static final String CHUNK = "#{jobParameters['chunkSize']}";
  private static final String THROTTLE = "#{jobParameters['throttleLimit']}";
  private static final String ETAG = "#{jobParameters['artist']}";
  private static final String ARTIST_STEP_FLOW = "artist step flow";
  private static final String ARTIST_FLOW_STEP = "artist flow step";
  private static final String ARTIST_CORE_STEP = "artist core step";
  private static final String ARTIST_SUB_ITEMS_STEP = "artist sub items step";
  private static final String ANY = "*";
  private static final String FAILED = "FAILED";

  private final StepBuilderFactory sbf;
  private final DiscogsDumpService dumpService;
  private final JpaItemWriter<Artist> artistItemWriter;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final ArtistSubItemsProcessor artistSubItemsProcessor;
  private final ItemWriter<Collection<BatchCommand>> artistSubItemWriter;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager tm;

  @Bean
  @JobScope
  // TODO: add file fetch and clear step
  public Step artistStep(@Value(CHUNK) Integer chunkSize, @Value(THROTTLE) Integer throttleLimit, @Value(ETAG) String artistETag) {
    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(ARTIST_STEP_FLOW)
            .next(artistCoreStep(chunkSize, throttleLimit))
            .next(artistSubItemsStep(chunkSize, throttleLimit))
            .build();
    FlowStep artistFlowStep = new FlowStep();
    artistFlowStep.setJobRepository(jobRepository);
    artistFlowStep.setName(ARTIST_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step artistCoreStep(
      @Value(CHUNK) Integer chunkSize, @Value(THROTTLE) Integer throttleLimit) {
    SynchronizedItemStreamReader<ArtistXML> artistDTOItemReader = artistStreamReader(null);
    return sbf.get(ARTIST_CORE_STEP)
        .<ArtistXML, Artist>chunk(chunkSize)
        .reader(artistDTOItemReader)
        .processor(artistDtoArtistProcessor())
        .writer(artistItemWriter)
        .faultTolerant()
        .retryLimit(10)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(new StringFieldNormalizingItemReadListener<>())
        .listener(new StopWatchStepExecutionListener())
        .throttleLimit(throttleLimit)
        .taskExecutor(taskExecutor)
        .transactionManager(tm)
        .build();
  }

  @Bean
  @JobScope
  public Step artistSubItemsStep(
      @Value(CHUNK) Integer chunkSize, @Value(THROTTLE) Integer throttleLimit) {
    return sbf.get(ARTIST_SUB_ITEMS_STEP)
        .<ArtistXML, Collection<BatchCommand>>chunk(chunkSize)
        .reader(artistStreamReader(null))
        .processor(artistSubItemsProcessor)
        .writer(artistSubItemWriter)
        .faultTolerant()
        .retryLimit(10)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(new StringFieldNormalizingItemReadListener<>())
        .listener(new StopWatchStepExecutionListener())
        .throttleLimit(throttleLimit)
        .build();
  }

  @Bean
  @StepScope
  public AsyncItemProcessor<ArtistXML, Collection<BatchCommand>> asyncArtistSubItemsProcessor()
      throws Exception {
    AsyncItemProcessor<ArtistXML, Collection<BatchCommand>> processor = new AsyncItemProcessor<>();
    processor.setDelegate(artistSubItemsProcessor);
    processor.setTaskExecutor(taskExecutor);
    processor.afterPropertiesSet();
    return processor;
  }

  @Bean
  @StepScope
  public AsyncItemProcessor<ArtistXML, Artist> asyncArtistProcessor() {
    AsyncItemProcessor<ArtistXML, Artist> processor = new AsyncItemProcessor<>();
    processor.setTaskExecutor(taskExecutor);
    processor.setDelegate(artistDtoArtistProcessor());
    return processor;
  }

  @Bean
  @StepScope
  public AsyncItemWriter<Artist> asyncArtistItemWriter() {
    AsyncItemWriter<Artist> writer = new AsyncItemWriter<>();
    writer.setDelegate(artistItemWriter);
    return writer;
  }

  @Bean
  @StepScope
  public AsyncItemWriter<Collection<BatchCommand>> asyncArtistSubItemsWriter() {
    AsyncItemWriter<Collection<BatchCommand>> writer = new AsyncItemWriter<>();
    writer.setDelegate(artistSubItemWriter);
    return writer;
  }

  @Bean
  @StepScope
  public SynchronizedItemStreamReader<ArtistXML> artistStreamReader(@Value(ETAG) String eTag) {
    try {
      return DumpItemReaderBuilder.build(ArtistXML.class, dumpService.getDiscogsDump(eTag));
    } catch (Exception e) {
      throw new InitializationFailureException(
          "failed to initialize artist stream reader: " + e.getMessage());
    }
  }

  @Bean
  @StepScope
  public ItemProcessor<ArtistXML, Artist> artistDtoArtistProcessor() {
    return dto -> null;
  }
}
