package io.dsub.discogsdata.batch.job.tasklet;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.exception.FileFetchException;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class FileFetchTaskletTest {

  Path filePath;
  Path targetPath;
  DiscogsDump fakeDump;
  FileFetchTasklet fileFetchTasklet;
  final StepExecution stepExecution = new StepExecution("step", new JobExecution(1L));
  final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
  final StepContribution stepContribution = new StepContribution(stepExecution);
  @RegisterExtension LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    filePath = Path.of(RandomString.make());
    targetPath = Path.of(RandomString.make());
    fakeDump = DiscogsDump.builder().size(1000L).uriString("t/t/" + targetPath).build();
    fileFetchTasklet = new FileFetchTasklet(fakeDump, filePath);
    try {
      Files.createFile(filePath);
      Files.write(filePath, RandomString.make(1000).getBytes());
    } catch (IOException e) {
      System.out.println(getClass().getSimpleName() + " failed due to IOException.");
      fail();
    }
  }

  @AfterEach
  void cleanUp() {
    try {
      Files.deleteIfExists(filePath);
      Files.deleteIfExists(targetPath);
    } catch (IOException e) {
      System.out.println(getClass().getSimpleName() + " failed due to IOException.");
      fail();
    }
  }

  @Test
  void whenExecuteWithNoPriorFile__ShouldProperlyCopyTheGivenItem() {
    try {
      RepeatStatus repeatStatus = fileFetchTasklet.execute(stepContribution, chunkContext);
      assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
      assertThat(Files.exists(targetPath)).isTrue();
      assertThat(Files.size(targetPath)).isEqualTo(Files.size(targetPath));
      assertThat(logSpy.getEvents().size()).isEqualTo(1);
      assertThat(logSpy.getEvents().get(0).getMessage())
          .isEqualTo("fetching " + targetPath + "...");
      assertThat(stepContribution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
      assertThat(chunkContext.isComplete()).isTrue();
    } catch (IOException e) {
      log.error("failed due to :" + e.getMessage());
      fail();
    }
  }

  @Test
  void whenExecuteWithIncompleteFile__ShouldProperlyCopyTheGivenItem() {
    try {
      Files.write(targetPath, RandomString.make(100).getBytes());
      RepeatStatus repeatStatus = fileFetchTasklet.execute(stepContribution, chunkContext);
      assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
      assertThat(Files.exists(targetPath)).isTrue();
      assertThat(Files.size(targetPath)).isEqualTo(Files.size(targetPath));
      assertThat(logSpy.getEvents().size()).isEqualTo(3);
      assertThat(logSpy.getEvents().get(0).getMessage())
          .isEqualTo("found duplicated file: " + targetPath + ". checking size...");
      assertThat(logSpy.getEvents().get(1).getMessage())
          .isEqualTo("incomplete size. deleting current file...");
      assertThat(logSpy.getEvents().get(2).getMessage())
          .isEqualTo("fetching " + targetPath + "...");
      assertThat(stepContribution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
      assertThat(chunkContext.isComplete()).isTrue();

    } catch (IOException e) {
      fail();
    }
  }

  @Test
  void whenExecuteWithCompleteFile__ShouldExitWithoutCopy() {
    try {
      Files.write(targetPath, RandomString.make(1000).getBytes());
      RepeatStatus repeatStatus = fileFetchTasklet.execute(stepContribution, chunkContext);
      assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
      assertThat(Files.exists(targetPath)).isTrue();
      assertThat(Files.size(targetPath)).isEqualTo(Files.size(targetPath));
      assertThat(logSpy.getEvents().size()).isEqualTo(2);
      assertThat(logSpy.getEvents().get(0).getMessage())
          .isEqualTo("found duplicated file: " + targetPath + ". checking size...");
      assertThat(logSpy.getEvents().get(1).getMessage())
          .isEqualTo("file already exists. proceeding...");
      assertThat(stepContribution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
      assertThat(chunkContext.isComplete()).isTrue();
    } catch (IOException e) {
      fail();
    }
  }

  @Test
  void whenExecute__AndDeleteThrows__ThenShouldUpdateStatusAsIntended() throws IOException {
    FileFetchTasklet tasklet = Mockito.spy(fileFetchTasklet);
    when(tasklet.execute(stepContribution, chunkContext)).thenCallRealMethod();
    Mockito.doThrow(FileFetchException.class).when(tasklet).deleteFile(any());
    Files.write(targetPath, RandomString.make(500).getBytes());
    Throwable t = catchThrowable(() -> tasklet.execute(stepContribution, chunkContext));
    assertThat(t).isNotNull().isInstanceOf(FileFetchException.class);
    assertThat(stepContribution.getExitStatus()).isEqualTo(ExitStatus.FAILED);
    assertThat(chunkContext.isComplete()).isTrue();
  }

  @Test
  void whenCheckFileSize__WithNonExistingFilePath__ShouldThrow() {
    try {
      Files.delete(filePath);
      // when
      Throwable t = catchThrowable(() -> fileFetchTasklet.checkFileSize(1000, filePath));
      // then
      assertThat(t)
              .isInstanceOf(FileFetchException.class)
              .hasMessageContaining("failed to check file size:")
              .hasMessageContaining(". reason:");
    } catch (IOException e) {
      fail("failed due to IOException: " + e.getMessage());
    }
  }

  @Test
  void whenCheckFileSize__WithDifferentFileSize__ShouldReturnFalse() {
    // when
    boolean result = fileFetchTasklet.checkFileSize(500, filePath);
    // then
    assertThat(result).isFalse();
  }

  @Test
  void whenCheckFileSize__WithSameFileSize__ShouldReturnTrue() {
    // when
    boolean result = fileFetchTasklet.checkFileSize(1000, filePath);
    // then
    assertThat(result).isTrue();
  }

  @Test
  void whenCheckFileSize__Throws__ShouldThrow() {
    String reason = "test";
    Path mockPath = Mockito.mock(Path.class);
    when(mockPath.getFileSystem())
        .thenThrow(new RuntimeException(reason));

    Throwable t = catchThrowable(() -> fileFetchTasklet.checkFileSize(1000, mockPath));
    assertThat(t.getMessage())
            .contains("failed to check file size:")
            .contains(". reason: test");
  }

  @Test
  void whenDeleteFile__Locked__ShouldThrow() {
    String reason = "test";
    Path mockPath = mock(Path.class);
    when(mockPath.getFileSystem()).thenThrow(new RuntimeException(reason));

    Throwable t = catchThrowable(() -> fileFetchTasklet.deleteFile(mockPath));
    assertThat(t.getMessage()).contains("failed to delete file:").contains("reason: test");
  }
}
