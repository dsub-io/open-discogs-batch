package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.listener.BatchListenerConfig;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.processor.ItemProcessorConfig;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.job.reader.ItemReaderConfig;
import io.dsub.discogs.batch.job.reconciliation.PostgreSQLMasterMainReleaseReconciler;
import io.dsub.discogs.batch.job.step.GlobalStepConfig;
import io.dsub.discogs.batch.job.tasklet.GenreStyleInsertionTasklet;
import io.dsub.discogs.batch.job.tasklet.MasterMainReleaseReconciliationTasklet;
import io.dsub.discogs.batch.job.writer.ItemWriterConfig;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import(
    value = {
        GlobalStepConfig.class,
        ItemReaderConfig.class,
        ItemProcessorConfig.class,
        ItemWriterConfig.class,
        BatchListenerConfig.class,
        ImportProgressStore.class,
        GenreStyleInsertionTasklet.class,
        PostgreSQLMasterMainReleaseReconciler.class,
        MasterMainReleaseReconciliationTasklet.class
    })
public class BatchInfrastructureConfig {

  private ApplicationArguments args;

  @Autowired
  public void setArgs(ApplicationArguments args) {
    this.args = args;
  }

  @Bean
  public Map<EntityType, DiscogsDump> dumpMap() {
    return new HashMap<>();
  }

  // TODO: test!
  @Bean
  public FileUtil fileUtil() {
    boolean cleanup = args.containsOption(ArgType.CLEANUP.getGlobalName());
    SimpleFileUtil.AppFileUtilBuilder builder = SimpleFileUtil.builder().isTemporary(cleanup);
    if (args.containsOption(ArgType.DATA_DIR.getGlobalName())) {
      builder.appDirectory(args.getOptionValues(ArgType.DATA_DIR.getGlobalName()).getFirst());
    }
    FileUtil fileUtil = builder.build();
    if (cleanup) {
      log.info("cleanup option applied. downloaded files will be removed after success.");
    } else {
      log.info("cleanup option not set. downloaded files will be kept.");
    }
    return fileUtil;
  }

  @Bean
  public DiscogsDumpItemReaderBuilder discogsDumpItemReaderBuilder() {
    return new DiscogsDumpItemReaderBuilder(fileUtil());
  }
}
