package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FileUtilConfigTest {

  ApplicationContextRunner ctx;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    ctx = new ApplicationContextRunner().withUserConfiguration(FileUtilConfig.class);
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
