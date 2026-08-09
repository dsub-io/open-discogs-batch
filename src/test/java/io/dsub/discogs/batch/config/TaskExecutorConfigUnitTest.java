package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.SynchronousQueue;

class TaskExecutorConfigUnitTest {

  @RegisterExtension private final LogSpy logSpy = new LogSpy();

  @Test
  void maxWorkersDefaultsToTheRuntimeProcessorAllocation() {
    int expected = Runtime.getRuntime().availableProcessors();

    new ApplicationContextRunner()
        .withUserConfiguration(TaskExecutorConfig.class)
        .withBean(ApplicationArguments.class, DefaultApplicationArguments::new)
        .run(
            context -> {
              assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
              ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutor.class);
              assertThat(executor.getCorePoolSize()).isEqualTo(expected);
              assertThat(executor.getMaxPoolSize()).isEqualTo(expected);
              assertThat(executor.getThreadPoolExecutor().getQueue())
                  .isInstanceOf(SynchronousQueue.class);
            });

    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .containsExactly("max-workers=" + expected + " (source=auto).");
  }

  @Test
  void configuredMaxWorkersIsUsedExactly() {
    new ApplicationContextRunner()
        .withUserConfiguration(TaskExecutorConfig.class)
        .withBean(
            ApplicationArguments.class,
            () -> new DefaultApplicationArguments("--maxWorkers=3"))
        .run(
            context -> {
              ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutor.class);
              assertThat(executor.getCorePoolSize()).isEqualTo(3);
              assertThat(executor.getMaxPoolSize()).isEqualTo(3);
              assertThat(executor.getThreadPoolExecutor().getQueue())
                  .isInstanceOf(SynchronousQueue.class);
            });

    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .containsExactly("max-workers=3 (source=configured).");
  }
}
