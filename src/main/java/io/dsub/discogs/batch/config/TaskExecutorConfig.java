package io.dsub.discogs.batch.config;

import static io.dsub.discogs.batch.argument.ArgType.MAX_WORKERS;

import io.dsub.discogs.batch.argument.PositiveIntegerParser;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class TaskExecutorConfig {
  private static final int DEFAULT_MAX_WORKERS = Runtime.getRuntime().availableProcessors();

  /**
   * Primary executor for concurrent chunk processing. Core and maximum pool sizes use the same
   * resolved {@code max-workers} value so concurrent chunk execution cannot exceed the configured
   * limit. A synchronous handoff plus blocking rejection policy applies producer backpressure
   * instead of retaining parsed chunks in an executor queue.
   *
   * <p>{@link ThreadPoolTaskExecutor#setWaitForTasksToCompleteOnShutdown(boolean)} is set to
   * true so in-flight chunks finish during graceful shutdown.
   *
   * @return instance of {@link ThreadPoolTaskExecutor}.
   */
  @Bean
  public ThreadPoolTaskExecutor batchTaskExecutor(ApplicationArguments args) {
    boolean configured = args.containsOption(MAX_WORKERS.getGlobalName());
    int maxWorkers = configured ? configuredMaxWorkers(args) : DEFAULT_MAX_WORKERS;
    log.info("max-workers={} (source={}).", maxWorkers, configured ? "configured" : "auto");
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(maxWorkers);
    taskExecutor.setMaxPoolSize(maxWorkers);
    taskExecutor.setQueueCapacity(0);
    taskExecutor.setRejectedExecutionHandler(new BlockingTaskSubmissionPolicy());
    taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
    return taskExecutor;
  }

  private int configuredMaxWorkers(ApplicationArguments args) {
    List<String> values = args.getOptionValues(MAX_WORKERS.getGlobalName());
    String value = values == null || values.isEmpty() ? null : values.getFirst();
    return PositiveIntegerParser.require("max-workers", value);
  }
}
