package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.dump.DiscogsDumpVerifier;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.util.FileUtil;
import java.util.concurrent.CountDownLatch;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

public class BatchInfrastructureConfigUnitTest {

  ApplicationContextRunner ctx;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    ctx = new ApplicationContextRunner()
        .withBean(DSLContext.class, () -> mock(DSLContext.class))
        .withBean(JobRepository.class, () -> mock(JobRepository.class))
        .withBean(CountDownLatch.class, () -> mock(CountDownLatch.class))
        .withBean(DiscogsDumpVerifier.class, () -> mock(DiscogsDumpVerifier.class))
        .withBean(DiscogsDumpService.class, () -> mock(DiscogsDumpService.class))
        .withBean(
            PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .withBean(ThreadPoolTaskExecutor.class, () -> mock(ThreadPoolTaskExecutor.class))
        .withUserConfiguration(BatchInfrastructureConfig.class);
  }

  @Test
  void givenCleanupOption__ShouldRemoveDownloadedFilesAfterSuccess() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class, "--cleanup");

    // when
    ctx.run(
        it -> {
          assertThat(it).hasSingleBean(FileUtil.class);
          assertThat(it.getBean(FileUtil.class).isTemporary()).isTrue();
        });

    // then
    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .hasSize(1)
        .first()
        .isEqualTo("cleanup option applied. downloaded files will be removed after success.");
  }

  @Test
  void givenOptionWithoutCleanup__ShouldKeepDownloadedFiles() {
    // given
    ctx = ctx.withBean(DefaultApplicationArguments.class);

    // when
    ctx.run(
        it -> {
          assertThat(it).hasSingleBean(FileUtil.class);
          assertThat(it.getBean(FileUtil.class).isTemporary()).isFalse();
        });

    // then
    assertThat(logSpy.getLogsByExactLevelAsString(Level.INFO, true))
        .hasSize(1)
        .first()
        .isEqualTo("cleanup option not set. downloaded files will be kept.");
  }
}
