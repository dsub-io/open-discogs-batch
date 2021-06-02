package io.dsub.discogsdata.batch.job.step;

import io.dsub.discogsdata.batch.BatchCommand;
import io.dsub.discogsdata.batch.domain.master.MasterBatchCommand.MasterArtistCommand;
import io.dsub.discogsdata.batch.domain.master.MasterBatchCommand.MasterCommand;
import io.dsub.discogsdata.batch.domain.master.MasterBatchCommand.MasterGenreCommand;
import io.dsub.discogsdata.batch.domain.master.MasterBatchCommand.MasterStyleCommand;
import io.dsub.discogsdata.batch.domain.master.MasterBatchCommand.MasterVideoCommand;
import io.dsub.discogsdata.batch.domain.master.MasterXML;
import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.batch.dump.service.DiscogsDumpService;
import io.dsub.discogsdata.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogsdata.batch.job.listener.StringFieldNormalizingItemReadListener;
import io.dsub.discogsdata.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogsdata.batch.job.tasklet.FileClearTasklet;
import io.dsub.discogsdata.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogsdata.batch.job.writer.ClassifierCompositeCollectionItemWriter;
import io.dsub.discogsdata.batch.query.JpaEntityQueryBuilder;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterArtist;
import io.dsub.discogsdata.common.entity.master.MasterGenre;
import io.dsub.discogsdata.common.entity.master.MasterStyle;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import io.dsub.discogsdata.common.repository.GenreRepository;
import io.dsub.discogsdata.common.repository.StyleRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class MasterStepConfig extends AbstractStepConfig {

  private static final String ETAG = "#{jobParameters['master']}";
  private static final String MASTER_STEP_FLOW = "master step flow";
  private static final String MASTER_FLOW_STEP = "master flow step";
  private static final String MASTER_CORE_STEP = "master core step";
  private static final String MASTER_SUB_ITEMS_STEP = "master sub items step";
  private static final String MASTER_FILE_FETCH_STEP = "master file fetch step";
  private static final String MASTER_FILE_CLEAR_STEP = "master file clear step";
  private static final String MASTER_PRE_STEP = "master pre step";

  private final JpaEntityQueryBuilder<BaseEntity> queryBuilder;
  private final GenreRepository genreRepository;
  private final StyleRepository styleRepository;
  private final DataSource dataSource;
  private final StepBuilderFactory sbf;
  private final DiscogsDumpService dumpService;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final Map<DumpType, DiscogsDump> dumpMap;

  @Bean
  @JobScope
  // TODO: add clear step
  public Step masterStep() {
    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(MASTER_STEP_FLOW)
            .from(masterFileFetchStep()).on(FAILED).end()
            .from(masterFileFetchStep()).on(ANY).to(masterGenreStyleStep())
            .from(masterGenreStyleStep()).on(FAILED).end()
            .from(masterGenreStyleStep()).on(ANY).to(masterCoreStep(null))
            .from(masterCoreStep(null)).on(FAILED).end()
            .from(masterCoreStep(null)).on(ANY).to(masterSubItemsStep(null))
            .from(masterSubItemsStep(null)).on(ANY).to(masterFileClearStep())
            .from(masterFileClearStep()).on(ANY).end()
            .build();
    FlowStep artistFlowStep = new FlowStep();
    artistFlowStep.setJobRepository(jobRepository);
    artistFlowStep.setName(MASTER_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public DiscogsDump masterDump(@Value(ETAG) String eTag) {
    DiscogsDump dump = dumpService.getDiscogsDump(eTag);
    dumpMap.put(DumpType.MASTER, dump);
    return dump;
  }

  @Bean
  @JobScope
  public Step masterFileFetchStep() {
    return sbf.get(MASTER_FILE_FETCH_STEP)
        .tasklet(new FileFetchTasklet(masterDump(null)))
        .build();
  }

  @Bean
  @JobScope
  public Step masterFileClearStep() {
    return sbf.get(MASTER_FILE_CLEAR_STEP)
        .tasklet(new FileClearTasklet(masterDump(null)))
        .build();
  }

  @Bean
  @JobScope
  public Step masterGenreStyleStep() {
    return sbf.get(MASTER_PRE_STEP)
        .tasklet((contribution, chunkContext) -> {
          SynchronizedItemStreamReader<MasterXML> reader = masterStreamReader();
          reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

          Set<String> genres = new HashSet<>();
          Set<String> styles = new HashSet<>();

          MasterXML masterXML = reader.read();
          while (masterXML != null) {
            if (masterXML.getGenres() != null) {
              genres.addAll(masterXML.getGenres());
            }
            if (masterXML.getStyles() != null) {
              styles.addAll(masterXML.getStyles());
            }
            masterXML = reader.read();
          }
          styleRepository
              .saveAll(styles.stream().map(style -> Style.builder().name(style).build()).collect(
                  Collectors.toList()));
          genreRepository
              .saveAll(genres.stream().map(genre -> Genre.builder().name(genre).build()).collect(
                  Collectors.toList()));

          contribution.setExitStatus(ExitStatus.COMPLETED);
          chunkContext.setComplete();
          return RepeatStatus.FINISHED;
        })
        .build();
  }


  @Bean
  @JobScope
  public Step masterCoreStep(
      @Value(CHUNK) Integer chunkSize) {
    return sbf.get(MASTER_CORE_STEP)
        .<MasterXML, MasterCommand>chunk(chunkSize)
        .reader(masterStreamReader())
        .processor(masterProcessor())
        .writer(masterWriter())
        .faultTolerant()
        .retryLimit(10)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(new StringFieldNormalizingItemReadListener<>())
        .listener(new StopWatchStepExecutionListener())
        .taskExecutor(taskExecutor)
        .transactionManager(transactionManager)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }

  @Bean
  @JobScope
  public Step masterSubItemsStep(
      @Value(CHUNK) Integer chunkSize) {
    return sbf.get(MASTER_SUB_ITEMS_STEP)
        .<MasterXML, Collection<BatchCommand>>chunk(chunkSize)
        .reader(masterStreamReader())
        .processor(masterSubItemsProcessor())
        .writer(masterSubItemsWriter())
        .faultTolerant()
        .retryLimit(10)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(new StringFieldNormalizingItemReadListener<>())
        .listener(new StopWatchStepExecutionListener())
        .taskExecutor(taskExecutor)
        .transactionManager(transactionManager)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }


  @Bean
  @StepScope
  public SynchronizedItemStreamReader<MasterXML> masterStreamReader() {
    try {
      return DiscogsDumpItemReaderBuilder.build(MasterXML.class, masterDump(null));
    } catch (Exception e) {
      throw new InitializationFailureException(
          "failed to initialize master stream reader: " + e.getMessage());
    }
  }

  @Bean
  @StepScope
  public ItemProcessor<MasterXML, MasterCommand> masterProcessor() {
    return xml -> MasterCommand.builder()
        .id(xml.getId())
        .dataQuality(xml.getDataQuality())
        .mainReleaseItem(null)
        .year(xml.getYear())
        .title(xml.getTitle())
        .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<MasterXML, Collection<BatchCommand>> masterSubItemsProcessor() {
    return xml -> {
      List<BatchCommand> commands = new ArrayList<>();

      xml.getArtists().stream()
          .map(artist -> MasterArtistCommand.builder()
              .master(xml.getId())
              .artist(artist.getId())
              .build())
          .forEach(commands::add);

      xml.getVideos().stream()
          .peek(video -> {
            if (video.getTitle() != null && video.getTitle().isBlank()) {
              video.setTitle(null);
            }
            if (video.getDescription() != null && video.getDescription().isBlank()) {
              video.setDescription(null);
            }
            if (video.getUrl() != null && video.getUrl().isBlank()) {
              video.setUrl(null);
            }
          })
          .map(video -> MasterVideoCommand.builder()
              .master(xml.getId())
              .url(video.getUrl())
              .description(video.getDescription())
              .title(video.getTitle())
              .build())
          .forEach(commands::add);

      xml.getGenres().stream()
          .filter(genre -> !genre.isBlank())
          .map(genre -> MasterGenreCommand.builder()
              .genre(genre)
              .master(xml.getId())
              .build())
          .forEach(commands::add);

      xml.getStyles().stream()
          .filter(style -> !style.isBlank())
          .map(style -> MasterStyleCommand.builder()
              .master(xml.getId())
              .style(style)
              .build())
          .forEach(commands::add);
      return commands;
    };
  }

  @Bean
  @StepScope
  ItemWriter<MasterCommand> masterWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(Master.class), dataSource);
  }

  @Bean
  @StepScope
  ItemWriter<Collection<BatchCommand>> masterSubItemsWriter() {
    ClassifierCompositeCollectionItemWriter<BatchCommand> writer =
        new ClassifierCompositeCollectionItemWriter<>();
    writer.setClassifier(
        (Classifier<BatchCommand, ItemWriter<? super BatchCommand>>) classifiable -> {
          if (classifiable instanceof MasterStyleCommand) {
            return masterStyleItemWriter();
          }
          if (classifiable instanceof MasterVideoCommand) {
            return masterVideoItemWriter();
          }
          if (classifiable instanceof MasterGenreCommand) {
            return masterGenreItemWriter();
          }
          return masterArtistItemWriter();
        });
    return writer;
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> masterStyleItemWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(MasterStyle.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> masterGenreItemWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(MasterGenre.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> masterArtistItemWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(MasterArtist.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> masterVideoItemWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(MasterVideo.class), dataSource);
  }
}
