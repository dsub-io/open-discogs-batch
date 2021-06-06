package io.dsub.discogsdata.batch.job.step;

import io.dsub.discogsdata.batch.BatchCommand;
import io.dsub.discogsdata.batch.domain.label.LabelBatchCommand.LabelReleaseCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemArtistCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemCreditedArtistCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemFormatCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemGenreCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemIdentifierCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemStyleCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemTrackCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemVideoCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseItemBatchCommand.ReleaseItemWorkCommand;
import io.dsub.discogsdata.batch.domain.release.ReleaseXML;
import io.dsub.discogsdata.batch.domain.release.ReleaseXML.Format;
import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.batch.dump.service.DiscogsDumpService;
import io.dsub.discogsdata.batch.job.listener.StopWatchStepExecutionListener;
import io.dsub.discogsdata.batch.job.listener.StringFieldNormalizingItemReadListener;
import io.dsub.discogsdata.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogsdata.batch.job.tasklet.FileClearTasklet;
import io.dsub.discogsdata.batch.job.tasklet.FileFetchTasklet;
import io.dsub.discogsdata.batch.job.writer.ClassifierCompositeCollectionItemWriter;
import io.dsub.discogsdata.batch.query.QueryBuilder;
import io.dsub.discogsdata.batch.util.DefaultMalformedDateParser;
import io.dsub.discogsdata.batch.util.FileUtil;
import io.dsub.discogsdata.batch.util.MalformedDateParser;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.label.LabelRelease;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import io.dsub.discogsdata.common.entity.release.ReleaseItemArtist;
import io.dsub.discogsdata.common.entity.release.ReleaseItemCreditedArtist;
import io.dsub.discogsdata.common.entity.release.ReleaseItemFormat;
import io.dsub.discogsdata.common.entity.release.ReleaseItemGenre;
import io.dsub.discogsdata.common.entity.release.ReleaseItemIdentifier;
import io.dsub.discogsdata.common.entity.release.ReleaseItemStyle;
import io.dsub.discogsdata.common.entity.release.ReleaseItemTrack;
import io.dsub.discogsdata.common.entity.release.ReleaseItemVideo;
import io.dsub.discogsdata.common.entity.release.ReleaseItemWork;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ReleaseItemStepConfig extends AbstractStepConfig {

  private static final String ETAG = "#{jobParameters['release']}";
  private static final String RELEASE_STEP_FLOW = "release step flow";
  private static final String RELEASE_FLOW_STEP = "release flow step";
  private static final String RELEASE_CORE_STEP = "release core step";
  private static final String RELEASE_SUB_ITEMS_STEP = "release sub items step";
  private static final String RELEASE_FILE_FETCH_STEP = "release file fetch step";
  private static final String RELEASE_FILE_CLEAR_STEP = "release file clear step";

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

  ///////////////////////////////////////////////////////////////////////////
  // STEPS
  ///////////////////////////////////////////////////////////////////////////

  @Bean
  @JobScope
  public Step releaseStep() {
    Flow artistStepFlow =
        new FlowBuilder<SimpleFlow>(RELEASE_STEP_FLOW)
            .from(releaseFileFetchStep()).on(FAILED).end()
            .from(releaseFileFetchStep()).on(ANY).to(releaseItemCoreStep(null))
            .from(releaseItemCoreStep(null)).on(FAILED).end()
            .from(releaseItemCoreStep(null)).on(ANY).to(releaseItemSubItemsStep(null))
            .from(releaseItemSubItemsStep(null)).on(ANY).to(releaseFileClearStep())
            .from(releaseFileClearStep()).on(ANY).end()
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
  public Step releaseItemCoreStep(
      @Value(CHUNK) Integer chunkSize) {
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
  public Step releaseItemSubItemsStep(
      @Value(CHUNK) Integer chunkSize) {
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
  public Step releaseFileFetchStep() {
    return sbf.get(RELEASE_FILE_FETCH_STEP)
        .tasklet(new FileFetchTasklet(releaseItemDump(null), fileUtil))
        .build();
  }

  @Bean
  @JobScope
  public Step releaseFileClearStep() {
    return sbf.get(RELEASE_FILE_CLEAR_STEP)
        .tasklet(new FileClearTasklet(fileUtil))
        .build();
  }

  @Bean
  @JobScope
  public DiscogsDump releaseItemDump(@Value(ETAG) String eTag) {
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
    return xml -> ReleaseItemCommand.builder()
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
            .map(albumArtist -> ReleaseItemArtistCommand.builder()
                .artist(albumArtist.getId())
                .releaseItem(xml.getReleaseId())
                .build())
            .forEach(commands::add);
      }
      if (xml.getCompanies() != null) {
        xml.getCompanies().stream()
            .map(company -> ReleaseItemWorkCommand.builder()
                .label(company.getId())
                .releaseItem(xml.getReleaseId())
                .work(normalizeString(company.getWork()))
                .build())
            .forEach(commands::add);
      }
      if (xml.getCreditedArtists() != null) {
        xml.getCreditedArtists().stream()
            .map(creditedArtist -> ReleaseItemCreditedArtistCommand.builder()
                .role(normalizeString(creditedArtist.getRole()))
                .artist(creditedArtist.getId())
                .releaseItem(xml.getReleaseId())
                .build())
            .forEach(commands::add);
      }
      if (xml.getFormats() != null) {
        xml.getFormats().stream()
            .map(format -> ReleaseItemFormatCommand.builder()
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
            .map(genre -> ReleaseItemGenreCommand.builder()
                .genre(genre)
                .releaseItem(xml.getReleaseId())
                .build())
            .forEach(commands::add);
      }
      if (xml.getStyles() != null) {
        xml.getStyles().stream()
            .filter(style -> !style.isBlank())
            .map(style -> ReleaseItemStyleCommand.builder()
                .style(style)
                .releaseItem(xml.getReleaseId())
                .build())
            .forEach(commands::add);
      }

      if (xml.getIdentifiers() != null) {
        xml.getIdentifiers().stream()
            .map(identifier -> ReleaseItemIdentifierCommand.builder()
                .releaseItem(xml.getReleaseId())
                .description(normalizeString(identifier.getDescription()))
                .type(normalizeString(identifier.getType()))
                .value(normalizeString(identifier.getValue()))
                .build())
            .forEach(commands::add);
      }

      if (xml.getLabels() != null) {
        xml.getLabels().stream()
            .map(label -> LabelReleaseCommand.builder()
                .categoryNotation(normalizeString(label.getCatno()))
                .label(label.getId())
                .releaseItem(xml.getReleaseId())
                .build())
            .forEach(commands::add);
      }

      if (xml.getTracks() != null) {
        xml.getTracks().stream()
            .map(track -> ReleaseItemTrackCommand.builder()
                .duration(normalizeString(track.getDuration()))
                .position(normalizeString(track.getPosition()))
                .title(normalizeString(track.getTitle()))
                .releaseItem(xml.getReleaseId())
                .build())
            .forEach(commands::add);
      }

      if (xml.getVideos() != null) {
        xml.getVideos().stream()
            .map(video -> ReleaseItemVideoCommand.builder()
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
        (Classifier<BatchCommand, ItemWriter<? super BatchCommand>>) classifiable -> {
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
        });

    return writer;
  }

  @Bean
  @StepScope
  public ItemWriter<ReleaseItemCommand> releaseItemWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItem.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemArtistWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemArtist.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemCreditedArtistWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemCreditedArtist.class),
        dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemGenreWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemGenre.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemStyleWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemStyle.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemVideoWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemVideo.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemWorkWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemWork.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemFormatWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemFormat.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemIdentifierWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemIdentifier.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> releaseItemTrackWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(ReleaseItemTrack.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> labelReleaseItemWriter() {
    return buildItemWriter(queryBuilder.getUpsertQuery(LabelRelease.class), dataSource);
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
}
