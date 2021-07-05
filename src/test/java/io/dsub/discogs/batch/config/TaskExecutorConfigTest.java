package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class TaskExecutorConfigTest {

  ApplicationContextRunner ctx;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  int maxCore = Runtime.getRuntime().availableProcessors();
  int defaultCoreSize = maxCore > 2 ? (int) (maxCore * 0.8) : 1;

  @BeforeEach
  void setUp() {
    ctx = new ApplicationContextRunner().withUserConfiguration(TaskExecutorConfig.class);
  }

  @Test
  void givenCoreCountArg__ShouldLogDetails() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class, "--coreCount=" + defaultCoreSize);

    // when
    ctx.run(
        it -> {
          assertThat(it).hasSingleBean(ThreadPoolTaskExecutor.class);
          ThreadPoolTaskExecutor taskExecutor = it.getBean(ThreadPoolTaskExecutor.class);
          assertThat(taskExecutor.getCorePoolSize()).isEqualTo(defaultCoreSize);
        });

    // then
    List<String> infoLogs = getInfoLogs();
    assertAll(
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("found core count argument")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("setting core count")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains(String.valueOf(defaultCoreSize))));
  }

  @Test
  void givenCoreCountArgExceedsFormulae__ShouldLogDetails() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class, "--coreCount=" + maxCore);

    // when
    ctx.run(
        it -> {
          assertThat(it).hasSingleBean(ThreadPoolTaskExecutor.class);
          ThreadPoolTaskExecutor taskExecutor = it.getBean(ThreadPoolTaskExecutor.class);
          assertThat(taskExecutor.getCorePoolSize()).isEqualTo(defaultCoreSize);
        });

    // then
    List<String> infoLogs = getInfoLogs();
    assertAll(
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("found core count argument")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("setting core count")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains(String.valueOf(defaultCoreSize))),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains(String.valueOf(maxCore))));
  }

  @Test
  void givenNoCoreCountArg__ShouldLogDetails() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class);
    // when
    ctx.run(
        it -> {
          assertThat(it).hasSingleBean(ThreadPoolTaskExecutor.class);
          ThreadPoolTaskExecutor taskExecutor = it.getBean(ThreadPoolTaskExecutor.class);
          assertThat(taskExecutor.getCorePoolSize()).isEqualTo(defaultCoreSize);
        });

    List<String> infoLogs = getInfoLogs();
    // then
    assertAll(
        () ->
            assertThat(infoLogs)
                .anyMatch(s -> s.contains("setting core count to " + defaultCoreSize)),
        () -> assertThat(infoLogs).noneMatch(s -> s.contains("found")));
  }

  List<String> getInfoLogs() {
    return logSpy.getLogsByExactLevelAsString(Level.INFO, true);
  }

  @Test
  void givenNegativeCoreCountArg__ShouldReturnDefaultValue() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class, "--coreCount=-1");

    // when
    ctx.run(
        it -> {
          assertThat(it).hasSingleBean(ThreadPoolTaskExecutor.class);
          ThreadPoolTaskExecutor taskExecutor = it.getBean(ThreadPoolTaskExecutor.class);
          assertThat(taskExecutor.getCorePoolSize()).isEqualTo(defaultCoreSize);
        });

    // then
    List<String> infoLogs = getInfoLogs();
    assertAll(
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("found core count argument")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("negative")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("-1")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains("setting core count")),
        () -> assertThat(infoLogs).anyMatch(s -> s.contains(String.valueOf(defaultCoreSize))));
  }
}
