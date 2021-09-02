package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.decider.MasterMainReleaseStepJobExecutionDecider;
import io.dsub.discogs.batch.job.listener.BatchListenerConfig;
import io.dsub.discogs.batch.job.processor.ItemProcessorConfig;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.job.reader.ItemReaderConfig;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.step.GlobalStepConfig;
import io.dsub.discogs.batch.job.tasklet.GenreStyleInsertionTasklet;
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
        GenreStyleInsertionTasklet.class
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

  @Bean
  public MasterMainReleaseStepJobExecutionDecider masterMainReleaseStepJobExecutionDecider(
      DefaultEntityIdRegistry registry) {
    return new MasterMainReleaseStepJobExecutionDecider(registry);
  }

  // TODO: test!
  @Bean
  public FileUtil fileUtil() {
    boolean keepFile = args.containsOption(ArgType.MOUNT.getGlobalName());
    FileUtil fileUtil = SimpleFileUtil.builder().isTemporary(!keepFile).build();
    if (keepFile) {
      log.info("detected mount option. keeping file...");
    } else {
      log.info("mount option not set. files will be removed after the job.");
    }
    return fileUtil;
  }

  @Bean
  public DiscogsDumpItemReaderBuilder discogsDumpItemReaderBuilder() {
    return new DiscogsDumpItemReaderBuilder(fileUtil());
  }
}
