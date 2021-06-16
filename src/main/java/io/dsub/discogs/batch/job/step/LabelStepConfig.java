package io.dsub.discogs.batch.job.step;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.batch.domain.label.LabelBatchCommand.LabelCommand;
import io.dsub.discogs.batch.domain.label.LabelBatchCommand.LabelSubLabelCommand;
import io.dsub.discogs.batch.domain.label.LabelBatchCommand.LabelUrlCommand;
import io.dsub.discogs.batch.domain.label.LabelXML;
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
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.entity.label.Label;
import io.dsub.discogs.common.entity.label.LabelSubLabel;
import io.dsub.discogs.common.entity.label.LabelUrl;
import java.util.Collection;
import java.util.LinkedList;
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
public class LabelStepConfig extends AbstractStepConfig<BatchCommand> {

  private static final String ETAG = "#{jobParameters['label']}";
  private static final String LABEL_STEP_FLOW = "label step flow";
  private static final String LABEL_FLOW_STEP = "label flow step";
  private static final String LABEL_CORE_STEP = "label core step";
  private static final String LABEL_SUB_ITEMS_STEP = "label sub items step";
  private static final String LABEL_FILE_FETCH_STEP = "label file fetch step";
  private static final String LABEL_FILE_CLEAR_STEP = "label file clear step";
  private static final String LABEL_TEMPORARY_TABLES_PRUNE_STEP =
      "label temporary tables prune step";
  private static final String LABEL_SELECT_INSERT_STEP = "label select insert step";

  private final QueryBuilder<BaseEntity> queryBuilder;
  private final DataSource dataSource;
  private final StepBuilderFactory sbf;
  private final DiscogsDumpService dumpService;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final DiscogsDumpItemReaderBuilder readerBuilder;
  private final FileUtil fileUtil;
  private final JdbcTemplate jdbcTemplate;
  private final Map<DumpType, DiscogsDump> dumpMap;
  private final List<Class<? extends BaseEntity>> entities =
      List.of(Label.class, LabelUrl.class, LabelSubLabel.class);

  @Bean
  @JobScope
  public Step labelStep(@Value(ETAG) String eTag)
      throws InvalidArgumentException, DumpNotFoundException {
    if (eTag == null || eTag.isBlank()) {
      return buildSkipStep(DumpType.LABEL, sbf);
    }

    Flow labelStepFlow =
        new FlowBuilder<SimpleFlow>(LABEL_STEP_FLOW)
            .from(labelFileFetchStep())
            .on(FAILED)
            .to(labelFileClearStep())
            .from(labelFileFetchStep())
            .on(ANY)
            .to(labelCoreStep(null))
            .from(labelCoreStep(null))
            .on(FAILED)
            .to(labelFileClearStep())
            .from(labelCoreStep(null))
            .on(ANY)
            .to(labelSubItemsStep(null))
            .from(labelSubItemsStep(null))
            .on(FAILED)
            .to(labelFileClearStep())
            .from(labelSubItemsStep(null))
            .on(ANY)
            .to(labelTemporaryTablesPruneStep())
            .from(labelTemporaryTablesPruneStep())
            .on(FAILED)
            .to(labelFileClearStep())
            .from(labelTemporaryTablesPruneStep())
            .on(ANY)
            .to(labelSelectInsertStep())
            .from(labelSelectInsertStep())
            .on(ANY)
            .to(labelFileClearStep())
            .from(labelFileClearStep())
            .on(ANY)
            .end()
            .build();
    FlowStep artistFlowStep = new FlowStep();
    artistFlowStep.setJobRepository(jobRepository);
    artistFlowStep.setName(LABEL_FLOW_STEP);
    artistFlowStep.setStartLimit(Integer.MAX_VALUE);
    artistFlowStep.setFlow(labelStepFlow);
    return artistFlowStep;
  }

  @Bean
  @JobScope
  public Step labelCoreStep(@Value(CHUNK) Integer chunkSize) throws InvalidArgumentException {
    return sbf.get(LABEL_CORE_STEP)
        .<LabelXML, LabelCommand>chunk(chunkSize)
        .reader(labelStreamReader())
        .processor(labelProcessor())
        .writer(labelWriter())
        .faultTolerant()
        .retryLimit(10)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(new StringFieldNormalizingItemReadListener<>())
        .listener(new StopWatchStepExecutionListener())
        .transactionManager(transactionManager)
        .taskExecutor(taskExecutor)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }

  @Bean
  @JobScope
  public Step labelSubItemsStep(@Value(CHUNK) Integer chunkSize) {
    return sbf.get(LABEL_SUB_ITEMS_STEP)
        .<LabelXML, Collection<BatchCommand>>chunk(chunkSize)
        .reader(labelStreamReader())
        .processor(labelSubItemProcessor())
        .writer(labelSubItemWriter())
        .faultTolerant()
        .retryLimit(10)
        .retry(DeadlockLoserDataAccessException.class)
        .listener(new StringFieldNormalizingItemReadListener<>())
        .listener(new StopWatchStepExecutionListener())
        .transactionManager(transactionManager)
        .taskExecutor(taskExecutor)
        .throttleLimit(taskExecutor.getMaxPoolSize())
        .build();
  }

  @Bean
  @JobScope
  public Step labelFileFetchStep() throws DumpNotFoundException {
    return sbf.get(LABEL_FILE_FETCH_STEP)
        .tasklet(new FileFetchTasklet(labelDump(null), fileUtil))
        .build();
  }

  @Bean
  @JobScope
  public Step labelFileClearStep() {
    return sbf.get(LABEL_FILE_CLEAR_STEP).tasklet(new FileClearTasklet(fileUtil)).build();
  }

  @Bean
  @JobScope
  public Step labelTemporaryTablesPruneStep() {
    List<String> queries =
        entities.stream()
            .filter(clazz -> clazz != Label.class)
            .map(queryBuilder::getPruneQuery)
            .collect(Collectors.toList());
    return sbf.get(LABEL_TEMPORARY_TABLES_PRUNE_STEP)
        .tasklet(new QueryExecutionTasklet(queries, jdbcTemplate))
        .build();
  }

  @Bean
  @JobScope
  public Step labelSelectInsertStep() {
    List<String> queries =
        entities.stream().map(queryBuilder::getSelectInsertQuery).collect(Collectors.toList());
    return sbf.get(LABEL_SELECT_INSERT_STEP)
        .tasklet(new QueryExecutionTasklet(queries, jdbcTemplate))
        .build();
  }

  @Bean
  @JobScope
  public DiscogsDump labelDump(@Value(ETAG) String eTag) throws DumpNotFoundException {
    DiscogsDump dump = dumpService.getDiscogsDump(eTag);
    dumpMap.put(DumpType.LABEL, dump);
    return dump;
  }

  @Bean
  @StepScope
  public SynchronizedItemStreamReader<LabelXML> labelStreamReader() {
    try {
      return readerBuilder.build(LabelXML.class, labelDump(null));
    } catch (Exception e) {
      throw new InitializationFailureException(
          "failed to initialize label stream reader: " + e.getMessage());
    }
  }

  @Bean
  @StepScope
  public ItemProcessor<LabelXML, LabelCommand> labelProcessor() {
    return xml ->
        LabelCommand.builder()
            .id(xml.getId())
            .name(xml.getName())
            .profile(xml.getProfile())
            .contactInfo(xml.getContactInfo())
            .dataQuality(xml.getDataQuality())
            .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<LabelXML, Collection<BatchCommand>> labelSubItemProcessor() {
    return xml -> {
      List<BatchCommand> batchCommands = new LinkedList<>();

      if (xml.getUrls() != null) {

        xml.getUrls().stream()
            .filter(url -> !url.isBlank())
            .map(url -> LabelUrlCommand.builder().url(url).label(xml.getId()).build())
            .forEach(batchCommands::add);
      }

      if (xml.getSubLabels() != null) {
        xml.getSubLabels().stream()
            .map(
                subLabel ->
                    LabelSubLabelCommand.builder()
                        .parent(xml.getId())
                        .subLabel(subLabel.getId())
                        .build())
            .forEach(batchCommands::add);
      }
      return batchCommands;
    };
  }

  @Bean
  @StepScope
  public ItemWriter<Collection<BatchCommand>> labelSubItemWriter() {
    ClassifierCompositeCollectionItemWriter<BatchCommand> writer =
        new ClassifierCompositeCollectionItemWriter<>();
    writer.setClassifier(
        (Classifier<BatchCommand, ItemWriter<? super BatchCommand>>)
            classifiable -> {
              try {
                return classify(classifiable);
              } catch (InvalidArgumentException e) {
                log.error(e.getMessage(), e);
                return null;
              }
            });
    return writer;
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> labelWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(Label.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> labelUrlWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(LabelUrl.class), dataSource);
  }

  @Bean
  @StepScope
  public ItemWriter<BatchCommand> labelSubLabelWriter() throws InvalidArgumentException {
    return buildItemWriter(queryBuilder.getTemporaryInsertQuery(LabelSubLabel.class), dataSource);
  }

  protected ItemWriter<? super BatchCommand> classify(BatchCommand classifiable)
      throws InvalidArgumentException {
    if (classifiable instanceof LabelSubLabelCommand) {
      return labelSubLabelWriter();
    }
    return labelUrlWriter();
  }
}
