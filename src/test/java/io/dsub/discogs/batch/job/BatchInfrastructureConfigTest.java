package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.util.FileUtil;
import java.util.concurrent.CountDownLatch;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class BatchInfrastructureConfigTest {

  ApplicationContextRunner ctx;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    ctx = new ApplicationContextRunner()
        .withBean(DSLContext.class, () -> mock(DSLContext.class))
        .withBean(JobRepository.class, () -> mock(JobRepository.class))
        .withBean(CountDownLatch.class, () -> mock(CountDownLatch.class))
        .withBean(DiscogsDumpService.class, () -> mock(DiscogsDumpService.class))
        .withBean(StepBuilderFactory.class, () -> mock(StepBuilderFactory.class))
        .withBean(ThreadPoolTaskExecutor.class, () -> mock(ThreadPoolTaskExecutor.class))
        .withUserConfiguration(BatchInfrastructureConfig.class);
  }

  @Test
  void givenMountOption__ShouldSetNotBeingTemporaryFile() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class, "--mount");

    // when
    ctx.run(it -> assertThat(it).hasSingleBean(FileUtil.class));

    // then
    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .hasSize(1)
        .first()
        .isEqualTo("detected mount option. keeping file...");
  }

  @Test
  void givenOptionWithoutMount__ShouldSetAsTemporaryFile() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class);

    // when
    ctx.run(it -> assertThat(it).hasSingleBean(FileUtil.class));

    // then
    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .hasSize(1)
        .first()
        .isEqualTo("mount option not set. files will be removed after the job.");
  }
}
