package io.dsub.discogs.batch.job.step;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.batch.domain.label.LabelBatchCommand.LabelReleaseCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemArtistCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemCreditedArtistCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemFormatCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemGenreCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemIdentifierCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemStyleCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemTrackCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemVideoCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemWorkCommand;
import io.dsub.discogs.batch.domain.release.ReleaseXML;
import io.dsub.discogs.batch.domain.release.ReleaseXML.Format;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InitializationFailureException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogs.batch.job.listener.StringFieldNormalizingItemReadListener;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.job.tasklet.FileClearTasklet;
import io.dsub.discogs.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogs.batch.job.tasklet.QueryExecutionTasklet;
import io.dsub.discogs.batch.job.writer.ClassifierCompositeCollectionItemWriter;
import io.dsub.discogs.batch.query.QueryBuilder;
import io.dsub.discogs.batch.util.DefaultMalformedDateParser;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.MalformedDateParser;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.entity.label.LabelRelease;
import io.dsub.discogs.common.entity.release.ReleaseItem;
import io.dsub.discogs.common.entity.release.ReleaseItemArtist;
import io.dsub.discogs.common.entity.release.ReleaseItemCreditedArtist;
import io.dsub.discogs.common.entity.release.ReleaseItemFormat;
import io.dsub.discogs.common.entity.release.ReleaseItemGenre;
import io.dsub.discogs.common.entity.release.ReleaseItemIdentifier;
import io.dsub.discogs.common.entity.release.ReleaseItemStyle;
import io.dsub.discogs.common.entity.release.ReleaseItemTrack;
import io.dsub.discogs.common.entity.release.ReleaseItemVideo;
import io.dsub.discogs.common.entity.release.ReleaseItemWork;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReleaseItemStepConfig extends AbstractStepConfig<BatchCommand> {

  private static final String ETAG = "#{jobParameters['release']}";
  private static final String RELEASE_STEP_FLOW = "release step flow";
  private static final String RELEASE_FLOW_STEP = "release flow step";
  private static final String RELEASE_CORE_STEP = "release core step";
  private static final String RELEASE_SUB_ITEMS_STEP = "release sub items step";
  private static final String RELEASE_FILE_FETCH_STEP = "release file fetch step";
  private static final String RELEASE_FILE_CLEAR_STEP = "release file clear step";
  private static final String RELEASE_TEMPORARY_TABLES_PRUNE_STEP =
      "release temporary tables prune step";
  private static final String RELEASE_SELECT_INSERT_STEP = "release select insert step";

  private final QueryBuilder<BaseEntity> queryBuilder;
  private final DataSource dataSource;
  private final StepBuilderFactory sbf;
  private final DiscogsDumpService dumpService;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final MalformedDateParser dateParser = new DefaultMalformedDateParser();
  private final DiscogsDumpItemReaderBuilder readerBuilder;
  private final FileUtil fileUtil;
  private final Map<DumpType, DiscogsDump> dumpMap;
  private final JdbcTemplate jdbcTemplate;
  private final List<Class<? extends BaseEntity>> entities =
      List.of(
          ReleaseItem.class,
          ReleaseItemArtist.class,
          ReleaseItemFormat.class,
          ReleaseItemCreditedArtist.class,
          ReleaseItemIdentifier.class,
          ReleaseItemTrack.class,
          ReleaseItemWork.class,
          ReleaseItemVideo.class,
          ReleaseItemFormat.class,
          ReleaseItemGenre.class,
          ReleaseItemStyle.class,
          LabelRelease.class);

  ///////////////////////////////////////////////////////////////////////////
  // STEPS
  ///////////////////////////////////////////////////////////////////////////

  @Bean
  @JobScope
  public Step releaseStep(@Value(ETAG) String eTag)
      throws InvalidArgumentException, DumpNotFoundException {
    if (eTag == null || eTag.isBlank()) {
      return buildSkipStep(DumpType.RELEASE, sbf);
    }

    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(RELEASE_STEP_FLOW)
            .from(releaseFileFetchStep())
            .on(FAILED)
            .to(releaseFileClearStep())
            .from(releaseFileFetchStep())
            .on(ANY)
            .to(releaseItemCoreStep(null))
            .from(releaseItemCoreStep(null))
            .on(FAILED)
            .to(releaseFileClearStep())
            .from(releaseItemCoreStep(null))
            .on(ANY)
            .to(releaseItemSubItemsStep(null))
            .from(releaseItemSubItemsStep(null))
            .on(FAILED)
            .to(releaseFileClearStep())
            .from(releaseItemSubItemsStep(null))
            .on(ANY)
            .to(releaseItemTemporaryTablesPruneStep())
            .from(releaseItemTemporaryTablesPruneStep())
            .on(FAILED)
            .to(releaseFileClearStep())
            .from(releaseItemTemporaryTablesPruneStep())
            .on(ANY)
            .to(releaseItemSelectInsertStep())
            .from(releaseItemSelectInsertStep())
            .on(ANY)
            .to(releaseFileClearStep())
            .from(releaseFileClearStep())
            .on(ANY)
            .end()
            .build();
    FlowStep artistFlowStep = new FlowStep();
    artistFlowStep.setJobRepository(jobRepository);
    artistFlowStep.setName(RELEASE_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(artistStepFlow);
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step releaseItemCoreStep(@Value(CHUNK) Integer chunkSize) throws InvalidArgumentException {
    return sbf.get(RELEASE_CORE_STEP)
        .<ReleaseXML, ReleaseItemCommand>chunk(chunkSize)
        .reader(releaseStreamReader())
        .processor(releaseItemProcessor())
        .writer(releaseItemWriter())
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
  public Step releaseItemSubItemsStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(RELEASE_SUB_ITEMS_STEP)
        .<ReleaseXML, Collection<BatchCommand>>chunk(200)
        .reader(releaseStreamReader())
        .processor(releaseItemSubItemsProcessor())
        .writer(releaseSubItemsWriter())
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
  public Step releaseFileFetchStep() throws DumpNotFoundException {
    return sbf.get(RELEASE_FILE_FETCH_STEP)
        .tasklet(new FileFetchTasklet(releaseItemDump(null), fileUtil))
        .build();
  }

  @Bean
  @JobScope
  public Step releaseFileClearStep() {
    return sbf.get(RELEASE_FILE_CLEAR_STEP).tasklet(new FileClearTasklet(fileUtil)).build();
  }

  @Bean
  @JobScope
  public Step releaseItemTemporaryTablesPruneStep() {
    List<String> queries =
        entities.stream()
            .filter(clazz -> clazz != ReleaseItem.class)
            .map(queryBuilder::getPruneQuery)
            .collect(Collectors.toList());
    return sbf.get(RELEASE_TEMPORARY_TABLES_PRUNE_STEP)
        .tasklet(new QueryExecutionTasklet(queries, jdbcTemplate))
        .build();
  }

  @Bean
  @JobScope
  public Step releaseItemSelectInsertStep() {
    List<String> queries =
        entities.stream().map(queryBuilder::getSelectInsertQuery).collect(Collectors.toList());
    return sbf.get(RELEASE_SELECT_INSERT_STEP)
        .tasklet(new QueryExecutionTasklet(queries, jdbcTemplate))
        .build();
  }

  @Bean
  @JobScope
  public DiscogsDump releaseItemDump(@Value(ETAG) String eTag) throws DumpNotFoundException {
    DiscogsDump dump = dumpService.getDiscogsDump(eTag);
    dumpMap.put(DumpType.RELEASE, dump);
    return dump;
  }

  ///////////////////////////////////////////////////////////////////////////
  // READER, PROCESSOR, WRITER
  ///////////////////////////////////////////////////////////////////////////
  @Bean
  @StepScope
  public SynchronizedItemStreamReader<ReleaseXML> releaseStreamReader() {
    try {
      return readerBuilder.build(ReleaseXML.class, releaseItemDump(null));
    } catch (Exception e) {
      throw new InitializationFailureException(
          "failed to initialize release stream reader: " + e.getMessage());
    }
  }

  @Bean
  @StepScope
  public ItemProcessor<ReleaseXML, ReleaseItemCommand> releaseItemProcessor() {
    return xml ->
        ReleaseItemCommand.builder()
            .id(xml.getReleaseId())
            .dataQuality(xml.getDataQuality())
            .country(xml.getCountry())
            .isMaster(xml.getMaster() != null && xml.getMaster().isMaster())
            .master(xml.getMaster() == null ? null : xml.getMaster().getMasterId())
            .status(xml.getStatus())
            .title(xml.getTitle())
            .notes(xml.getNotes())
            .listedReleaseDate(xml.getReleaseDate())
            .hasValidDay(dateParser.isDayValid(xml.getReleaseDate()))
            .hasValidMonth(dateParser.isMonthValid(xml.getReleaseDate()))
            .hasValidYear(dateParser.isYearValid(xml.getReleaseDate()))
            .releaseDate(dateParser.parse(xml.getReleaseDate()))
            .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<ReleaseXML, Collection<BatchCommand>> releaseItemSubItemsProcessor() {
    return xml -> {
      List<BatchCommand> commands = new ArrayList<>();
      if (xml.getAlbumArtists() != null) {
        xml.getAlbumArtists().stream()
            .map(
                albumArtist ->
                    ReleaseItemArtistCommand.builder()
                        .artist(albumArtist.getId())
                        .releaseItem(xml.getReleaseId())
                        .build())
            .forEach(commands::add);
      }
      if (xml.getCompanies() != null) {
        xml.getCompanies().stream()
            .map(
                company ->
                    ReleaseItemWorkCommand.builder()
                        .label(company.getId())
                        .releaseItem(xml.getReleaseId())
                        .work(normalizeString(company.getWork()))
                        .build())
            .forEach(commands::add);
      }
      if (xml.getCreditedArtists() != null) {
        xml.getCreditedArtists().stream()
            .map(
                creditedArtist ->
                    ReleaseItemCreditedArtistCommand.builder()
                        .role(normalizeString(creditedArtist.getRole()))
                        .artist(creditedArtist.getId())
                        .releaseItem(xml.getReleaseId())
                        .build())
            .forEach(commands::add);
      }
      if (xml.getFormats() != null) {
        xml.getFormats().stream()
            .map(
                format ->
                    ReleaseItemFormatCommand.builder()
                        .releaseItem(xml.getReleaseId())
                        .quantity(format.getQty())
                        .text(normalizeString(format.getText()))
                        .name(normalizeString(format.getName()))
                        .description(normalizeString(getFormatDescription(format)))
                        .build())
            .forEach(commands::add);
      }
      if (xml.getGenres() != null) {
        xml.getGenres().stream()
            .filter(genre -> !genre.isBlank())
            .map(
                genre ->
                    ReleaseItemGenreCommand.builder()
                        .genre(genre)
                        .releaseItem(xml.getReleaseId())
                        .build())
            .forEach(commands::add);
      }
      if (xml.getStyles() != null) {
        xml.getStyles().stream()
            .filter(style -> !style.isBlank())
            .map(
                style ->
                    ReleaseItemStyleCommand.builder()
                        .style(style)
                        .releaseItem(xml.getReleaseId())
                        .build())
            .forEach(commands::add);
      }

      if (xml.getIdentifiers() != null) {
        xml.getIdentifiers().stream()
            .map(
                identifier ->
                    ReleaseItemIdentifierCommand.builder()
                        .releaseItem(xml.getReleaseId())
                        .description(normalizeString(identifier.getDescription()))
                        .type(normalizeString(identifier.getType()))
                        .value(normalizeString(identifier.getValue()))
                        .build())
            .forEach(commands::add);
      }

      if (xml.getLabels() != null) {
        xml.getLabels().stream()
            .map(
                label ->
                    LabelReleaseCommand.builder()
                        .categoryNotation(normalizeString(label.getCatno()))
                        .label(label.getId())
                        .releaseItem(xml.getReleaseId())
                        .build())
            .forEach(commands::add);
      }

      if (xml.getTracks() != null) {
        xml.getTracks().stream()
            .map(
                track ->
                    ReleaseItemTrackCommand.builder()
                        .duration(normalizeString(track.getDuration()))
                        .position(normalizeString(track.getPosition()))
                        .title(normalizeString(track.getTitle()))
                        .releaseItem(xml.getReleaseId())
                        .build())
            .forEach(commands::add);
      }

      if (xml.getVideos() != null) {
        xml.getVideos().stream()
            .map(
                video ->
                    ReleaseItemVideoCommand.builder()
                        .title(normalizeString(video.getTitle()))
                        .description(normalizeString(video.getDescription()))
                        .url(normalizeString(video.getUrl()))
                        .releaseItem(xml.getReleaseId())
                        .build())
            .forEach(commands::add);
      }

      return commands;
    };
  }

  @Bean
  @StepScope
  public ItemWriter<Collection<BatchCommand>> releaseSubItemsWriter() {
    ClassifierCompositeCollectionItemWriter<BatchCommand> writer =
        new ClassifierCompositeCollectionItemWriter<>();

    writer.setClassifier(
        (Classifier<BatchCommand, ItemWriter<? super BatchCommand>>)
            classifiable -> {
              try {
                return classify(classifiable);
              } catch (InvalidArgumentException e) {
                log.error(e.getMessage(), e);
              }
              return null;
            });

    return writer;
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(ReleaseItem.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemArtistWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemArtist.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemCreditedArtistWriter()
      throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemCreditedArtist.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemGenreWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemGenre.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemStyleWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemStyle.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemVideoWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemVideo.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemWorkWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(ReleaseItemWork.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemFormatWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemFormat.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemIdentifierWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemIdentifier.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemTrackWriter() throws InvalidArgumentException {
    return buildItemWriter(
        queryBuilder.getTemporaryInsertQuery(ReleaseItemTrack.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> labelReleaseItemWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(LabelRelease.class), dataSource);
  }

  private String normalizeString(String input) {
    if (input != null && input.isBlank()) {
      return null;
    }
    return input;
  }

  private String getFormatDescription(Format format) {
    if (format.getDescription() == null) {
      return null;
    }
    return format.getDescription().stream()
        .map(desc -> "[d:" + desc + "]")
        .collect(Collectors.joining(","));
  }

  @Override
  protected ItemWriter<? super BatchCommand> classify(BatchCommand classifiable)
      throws InvalidArgumentException {
    if (classifiable instanceof ReleaseItemArtistCommand) {
      return releaseItemArtistWriter();
    }
    if (classifiable instanceof ReleaseItemCreditedArtistCommand) {
      return releaseItemCreditedArtistWriter();
    }
    if (classifiable instanceof ReleaseItemGenreCommand) {
      return releaseItemGenreWriter();
    }
    if (classifiable instanceof ReleaseItemStyleCommand) {
      return releaseItemStyleWriter();
    }
    if (classifiable instanceof ReleaseItemVideoCommand) {
      return releaseItemVideoWriter();
    }
    if (classifiable instanceof ReleaseItemTrackCommand) {
      return releaseItemTrackWriter();
    }
    if (classifiable instanceof ReleaseItemFormatCommand) {
      return releaseItemFormatWriter();
    }
    if (classifiable instanceof ReleaseItemIdentifierCommand) {
      return releaseItemIdentifierWriter();
    }
    if (classifiable instanceof LabelReleaseCommand) {
      return labelReleaseItemWriter();
    }
    return releaseItemWorkWriter();
  }
}
