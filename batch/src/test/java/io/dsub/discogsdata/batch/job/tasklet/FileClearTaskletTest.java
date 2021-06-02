package io.dsub.discogsdata.batch.job.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.exception.FileClearException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

class FileClearTaskletTest {

  final StepExecution stepExecution = new StepExecution("step", new JobExecution(1L));
  final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
  final StepContribution stepContribution = new StepContribution(stepExecution);

  FileClearTasklet fileClearTasklet;

  @TempDir
  Path tmpDir;

  Path filePath;

  DiscogsDump dump;

  @BeforeEach
  void setUp() {
    filePath = tmpDir.resolve(RandomString.make());
    dump = Mockito.mock(DiscogsDump.class);
    when(dump.getFileName()).thenReturn(RandomString.make());
    doCallRealMethod().when(dump).getResourcePath();
    fileClearTasklet = new FileClearTasklet(dump);
  }

  @AfterEach
  void cleanUp() {
    try {
      Files.deleteIfExists(dump.getResourcePath());
    } catch (IOException e) {
      fail(e);
    }
  }

  @Test
  void whenFileExists__ShouldDelete() {
    // given
    Path path = dump.getResourcePath();

    // when
    fileClearTasklet.execute(stepContribution, chunkContext);

    // then
    assertThat(path).doesNotExist();
    assertThat(chunkContext.isComplete()).isTrue();
    assertThat(stepContribution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
  }

  @Test
  void whenFileDoesNotExists__ShouldNotThrow() {
    // given
    Path path = Path.of("i am blank");
    doReturn(path).when(dump).getResourcePath();

    // when
    fileClearTasklet.execute(stepContribution, chunkContext);

    // then
    assertThat(filePath).doesNotExist();
    assertThat(chunkContext.isComplete()).isTrue();
    assertThat(stepContribution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
  }

  @Test
  void whenFileAccessThrows__ShouldHandleIt() {
    // given
    Path mockPath = mock(Path.class);
    when(mockPath.getFileSystem()).thenThrow(new RuntimeException("reason"));
    doReturn(mockPath).when(dump).getResourcePath();

    // when
    Throwable t = catchThrowable(() -> fileClearTasklet.execute(stepContribution, chunkContext));

    // then
    assertThat(t)
        .isInstanceOf(FileClearException.class)
        .hasMessageStartingWith("failed to delete file");

    // extra clear
    doReturn(filePath).when(dump).getResourcePath();
  }
}