package io.dsub.discogs.batch.job.step;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.batch.domain.artist.ArtistBatchCommand.ArtistAliasCommand;
import io.dsub.discogs.batch.domain.artist.ArtistBatchCommand.ArtistCommand;
import io.dsub.discogs.batch.domain.artist.ArtistBatchCommand.ArtistGroupCommand;
import io.dsub.discogs.batch.domain.artist.ArtistBatchCommand.ArtistMemberCommand;
import io.dsub.discogs.batch.domain.artist.ArtistBatchCommand.ArtistNameVariationCommand;
import io.dsub.discogs.batch.domain.artist.ArtistBatchCommand.ArtistUrlCommand;
import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogs.batch.job.listener.StringFieldNormalizingItemReadListener;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.job.tasklet.FileClearTasklet;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.job.tasklet.QueryExecutionTasklet;
import io.dsub.discogs.batch.query.QueryBuilder;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.common.entity.artist.Artist;
import io.dsub.discogs.common.entity.artist.ArtistAlias;
import io.dsub.discogs.common.entity.artist.ArtistGroup;
import io.dsub.discogs.common.entity.artist.ArtistMember;
import io.dsub.discogs.common.entity.artist.ArtistNameVariation;
import io.dsub.discogs.common.entity.artist.ArtistUrl;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.exception.InitializationFailureException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistStepConfig extends AbstractStepConfig<BatchCommand> {

  private static final String ETAG = "#{jobParameters['artist']}";
  private static final String ARTIST_STEP_FLOW = "artist step flow";
  private static final String ARTIST_FLOW_STEP = "artist flow step";
  private static final String ARTIST_CORE_STEP = "artist core step";
  private static final String ARTIST_SUB_ITEMS_STEP = "artist sub items step";
  private static final String ARTIST_FILE_FETCH_STEP = "artist file fetch step";
  private static final String ARTIST_FILE_CLEAR_STEP = "artist file clear step";
  private static final String ARTIST_TEMPORARY_TABLES_PRUNE_STEP = "artist temporary tables prune step";
  private static final String ARTIST_SELECT_INSERT_STEP = "artist select insert step";

  private final QueryBuilder<BaseEntity> queryBuilder;
  private final DataSource dataSource;
  private final StepBuilderFactory sbf;
  private final DiscogsDumpService dumpService;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final DiscogsDumpItemReaderBuilder readerBuilder;
  private final Map<DumpType, DiscogsDump> dumpMap;
  private final FileUtil fileUtil;
  private final JdbcTemplate jdbcTemplate;
  private final List<Class<? extends BaseEntity>> entityClasses =
      List.of(
          Artist.class,
          ArtistAlias.class,
          ArtistMember.class,
          ArtistGroup.class,
          ArtistNameVariation.class,
          ArtistUrl.class);

  ///////////////////////////////////////////////////////////////////////////
  // STEPS
  ///////////////////////////////////////////////////////////////////////////

  @Bean
  @JobScope
  public Step artistStep(@Value(ETAG) String eTag)
      throws InvalidArgumentException, DumpNotFoundException {

    if (eTag == null || eTag.isBlank()) {
      return buildSkipStep(DumpType.ARTIST, sbf);
    }

    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(ARTIST_STEP_FLOW)
            .from(artistFileFetchStep())
            .on(FAILED)
            .to(artistFileClearStep())
            .from(artistFileFetchStep())
            .on(ANY)
            .to(artistCoreStep(null))
            .from(artistCoreStep(null))
            .on(FAILED)
            .to(artistFileClearStep())
            .from(artistCoreStep(null))
            .on(ANY)
            .to(artistSubItemsStep(null))
            .from(artistSubItemsStep(null))
            .on(FAILED)
            .to(artistFileClearStep())
            .from(artistSubItemsStep(null))
            .on(ANY)
            .to(artistTemporaryTablesPruneStep())
            .from(artistTemporaryTablesPruneStep())
            .on(FAILED)
            .to(artistFileClearStep())
            .from(artistTemporaryTablesPruneStep())
            .on(ANY)
            .to(artistSelectInsertStep())
            .from(artistSelectInsertStep())
            .on(ANY)
            .to(artistFileClearStep())
            .from(artistFileClearStep())
            .on(ANY)
            .end()
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
  public Step artistCoreStep(@Value(CHUNK) Integer chunkSize) throws InvalidArgumentException {
    return sbf.get(ARTIST_CORE_STEP)
        .<ArtistXML, BatchCommand>chunk(chunkSize)
        .reader(artistStreamReader())
        .processor(artistItemProcessor())
        .writer(artistItemWriter())
        .faultTolerant()
        .retryLimit(100)
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
  public Step artistSubItemsStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(ARTIST_SUB_ITEMS_STEP)
        .<ArtistXML, Collection<BatchCommand>>chunk(chunkSize)
        .reader(artistStreamReader())
        .processor(artistSubItemProcessor())
        .writer(artistSubItemsWriter())
        .faultTolerant()
        .retryLimit(100)
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
  public DiscogsDump artistDump(@Value(ETAG) String eTag) throws DumpNotFoundException {
    DiscogsDump dump = dumpService.getDiscogsDump(eTag);
    dumpMap.put(DumpType.ARTIST, dump);
    return dump;
  }

  @Bean
  @JobScope
  public Step artistFileFetchStep() throws DumpNotFoundException {
    return sbf.get(ARTIST_FILE_FETCH_STEP)
        .tasklet(new FileFetchTasklet(artistDump(null), fileUtil))
        .build();
  }

  @Bean
  @JobScope
  public Step artistFileClearStep() {
    return sbf.get(ARTIST_FILE_CLEAR_STEP).tasklet(new FileClearTasklet(fileUtil)).build();
  }

  @Bean
  @JobScope
  public Step artistTemporaryTablesPruneStep() {
    List<String> queries = entityClasses
            .stream()
            .filter(clazz -> clazz != Artist.class)
            .map(queryBuilder::getPruneQuery).collect(Collectors.toList());
    return sbf.get(ARTIST_TEMPORARY_TABLES_PRUNE_STEP)
        .tasklet(new QueryExecutionTasklet(queries, jdbcTemplate))
        .build();
  }

  @Bean
  @JobScope
  public Step artistSelectInsertStep() {
    List<String> queries =
        entityClasses.stream().map(queryBuilder::getSelectInsertQuery).collect(Collectors.toList());
    return sbf.get(ARTIST_SELECT_INSERT_STEP)
        .tasklet(new QueryExecutionTasklet(queries, jdbcTemplate))
        .build();
  }

  ///////////////////////////////////////////////////////////////////////////
  // READER, PROCESSOR, WRITER
  ///////////////////////////////////////////////////////////////////////////

  @Bean
  @StepScope
  public SynchronizedItemStreamReader<ArtistXML> artistStreamReader() {
    try {
      return readerBuilder.build(ArtistXML.class, artistDump(null));
    } catch (Exception e) {
      throw new InitializationFailureException(
          "failed to initialize artist stream reader: " + e.getMessage());
    }
  }

  @Bean
  @StepScope
  public ItemProcessor<ArtistXML, BatchCommand> artistItemProcessor() {
    return xml ->
        ArtistCommand.builder()
            .id(xml.getId())
            .name(xml.getName())
            .realName(xml.getRealName())
            .dataQuality(xml.getDataQuality())
            .profile(xml.getProfile())
            .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<ArtistXML, Collection<BatchCommand>> artistSubItemProcessor() {
    return xml -> {
      List<BatchCommand> batchCommands = new LinkedList<>();
      if (xml.getAliases() != null) {
        xml.getAliases().stream()
            .map(
                alias ->
                    ArtistAliasCommand.builder().alias(alias.getId()).artist(xml.getId()).build())
            .forEach(batchCommands::add);
      }

      if (xml.getGroups() != null) {
        xml.getGroups().stream()
            .map(
                group ->
                    ArtistGroupCommand.builder().artist(xml.getId()).group(group.getId()).build())
            .forEach(batchCommands::add);
      }

      if (xml.getMembers() != null) {
        xml.getMembers().stream()
            .map(
                member ->
                    ArtistMemberCommand.builder()
                        .artist(xml.getId())
                        .member(member.getId())
                        .build())
            .forEach(batchCommands::add);
      }

      if (xml.getUrls() != null) {
        xml.getUrls().stream()
            .filter(url -> !url.isBlank())
            .map(url -> ArtistUrlCommand.builder().artist(xml.getId()).url(url).build())
            .forEach(batchCommands::add);
      }

      if (xml.getNameVariations() != null) {
        xml.getNameVariations().stream()
            .filter(name -> !name.isBlank())
            .map(
                name -> ArtistNameVariationCommand.builder().artist(xml.getId()).name(name).build())
            .forEach(batchCommands::add);
      }
      return batchCommands;
    };
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> artistItemWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(Artist.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<Collection<BatchCommand>> artistSubItemsWriter() {
    return getClassifierCollectionItemWriter();
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> artistMemberItemWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(ArtistMember.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> artistGroupItemWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(ArtistGroup.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> artistAliasItemWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(ArtistAlias.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> artistUrlItemWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ArtistUrl.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> artistNameVariationItemWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ArtistNameVariation.class), dataSource);
  }

  protected ItemWriter<? super BatchCommand> classify(BatchCommand classifiable)
      throws InvalidArgumentException {
    if (classifiable instanceof ArtistMemberCommand) {
      return artistMemberItemWriter();
    }
    if (classifiable instanceof ArtistGroupCommand) {
      return artistGroupItemWriter();
    }
    if (classifiable instanceof ArtistAliasCommand) {
      return artistAliasItemWriter();
    }
    if (classifiable instanceof ArtistUrlCommand) {
      return artistUrlItemWriter();
    }
    return artistNameVariationItemWriter();
  }
}
