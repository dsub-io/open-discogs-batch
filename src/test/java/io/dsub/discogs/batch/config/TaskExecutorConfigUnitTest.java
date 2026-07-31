package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class TaskExecutorConfigUnitTest {

  @RegisterExtension private final LogSpy logSpy = new LogSpy();

  @Test
  void workerCountUsesTheContainerVisibleProcessorCount() {
    int available = Runtime.getRuntime().availableProcessors();
    int expected = available > 2 ? Math.max(1, (int) (available * 0.8)) : 1;

    new ApplicationContextRunner()
        .withUserConfiguration(TaskExecutorConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
              ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutor.class);
              assertThat(executor.getCorePoolSize()).isEqualTo(expected);
              assertThat(executor.getMaxPoolSize()).isEqualTo(expected);
            });

    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .containsExactly("setting worker count to " + expected + ".");
  }
}
