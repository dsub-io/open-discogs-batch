package io.dsub.discogsdata.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

  public static final int CORE_SIZE = Runtime.getRuntime().availableProcessors();
  public static final int DEFAULT_CHUNK_SIZE = 1000;
  public static final int DEFAULT_THROTTLE_LIMIT = CORE_SIZE > 2 ? (int) (CORE_SIZE * 0.8) : 1;

  /**
   * Primary bean for batch processing. The core and max pool size is fit into same value, just as
   * the same as the core size of host processor count.
   *
   * <p>{@link ThreadPoolTaskExecutor#setWaitForTasksToCompleteOnShutdown(boolean)}} is set to
   * true, in case of additional tasks required after the actual job is doe.
   *
   * @return instance of {@link ThreadPoolTaskExecutor}.
   */

  @Bean
  public ThreadPoolTaskExecutor batchTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(CORE_SIZE);
    taskExecutor.setMaxPoolSize(CORE_SIZE);
    taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
    taskExecutor.afterPropertiesSet();
    taskExecutor.initialize();
    return taskExecutor;
  }
}