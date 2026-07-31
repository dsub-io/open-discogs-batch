package io.dsub.discogs.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class TaskExecutorConfig {
  private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
  private static final int DEFAULT_THROTTLE_LIMIT =
      AVAILABLE_PROCESSORS > 2 ? Math.max(1, (int) (AVAILABLE_PROCESSORS * 0.8)) : 1;

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
  public ThreadPoolTaskExecutor batchTaskExecutor() {
    int coreCount = DEFAULT_THROTTLE_LIMIT;
    log.info("setting worker count to {}.", coreCount);
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(coreCount);
    taskExecutor.setMaxPoolSize(coreCount);
    taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
    taskExecutor.afterPropertiesSet();
    taskExecutor.initialize();
    return taskExecutor;
  }
}
