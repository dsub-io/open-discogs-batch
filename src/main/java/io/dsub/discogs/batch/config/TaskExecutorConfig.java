package io.dsub.discogs.batch.config;

import static io.dsub.discogs.batch.argument.ArgType.CORE_COUNT;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class TaskExecutorConfig {

  private static final int CORE_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int DEFAULT_THROTTLE_LIMIT = CORE_SIZE > 2 ? (int) (CORE_SIZE * 0.8) : 1;

  /**
   * Primary bean for batch processing. The core and max pool size is fit into same value, just as
   * the same as the core size of host processor count.
   *
   * <p>{@link ThreadPoolTaskExecutor#setWaitForTasksToCompleteOnShutdown(boolean)}} is set to
   * true,
   * in case of additional tasks required after the actual job is doe.
   *
   * @return instance of {@link ThreadPoolTaskExecutor}.
   */
  @Bean
  public ThreadPoolTaskExecutor batchTaskExecutor(ApplicationArguments args) {
    int coreCount = getCoreCount(args);
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(coreCount);
    taskExecutor.setMaxPoolSize(coreCount);
    taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
    taskExecutor.afterPropertiesSet();
    taskExecutor.initialize();
    return taskExecutor;
  }

  private int getCoreCount(ApplicationArguments args) {
    int coreCount = DEFAULT_THROTTLE_LIMIT;
    if (args.containsOption(CORE_COUNT.getGlobalName())) {
      int givenCnt = Integer.parseInt(args.getOptionValues(CORE_COUNT.getGlobalName()).get(0));
      log.info("found core count argument: {}", givenCnt);
      coreCount = getValidatedCoreCount(givenCnt);
    }
    log.info("setting core count to {}.", coreCount);
    return coreCount;
  }

  private int getValidatedCoreCount(int givenCount) {

    int coreCount = DEFAULT_THROTTLE_LIMIT;

    if (givenCount > DEFAULT_THROTTLE_LIMIT) {
      log.info(
          "given core count {} exceeds limit. reducing core usage to {} from throttle..",
          givenCount,
          DEFAULT_THROTTLE_LIMIT);
    } else if (givenCount < 0) {
      log.info(
          "core count found negative value: {}. using default core setting: {}.",
          givenCount,
          DEFAULT_THROTTLE_LIMIT);
    } else {
      coreCount = givenCount;
    }
    return coreCount;
  }
}
