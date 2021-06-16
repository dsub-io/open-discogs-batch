package io.dsub.discogs.batch.job.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.exception.FileDeleteException;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.util.FileUtil;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
class FileFetchTaskletTest {

  @Mock ChunkContext chunkContext;

  @Mock StepContribution stepContribution;

  @Mock DiscogsDump dump;

  @Mock FileUtil fileUtil;

  InputStream inputStream;

  @InjectMocks FileFetchTasklet fileFetchTasklet;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @Captor ArgumentCaptor<String> nameCaptor;

  @Captor ArgumentCaptor<String> msgCaptor;

  @Captor ArgumentCaptor<InputStream> inCaptor;

  private AutoCloseable closeable;

  @BeforeEach
  void setUp() throws IOException {
    closeable = MockitoAnnotations.openMocks(this);
    fileFetchTasklet = Mockito.spy(fileFetchTasklet);

    doReturn(RandomString.make()).when(dump).getFileName();
    doReturn(1000L).when(dump).getSize();

    inputStream = mock(InputStream.class);

    doReturn(inputStream).when(dump).getInputStream();

    nameCaptor = ArgumentCaptor.forClass(String.class);
    inCaptor = ArgumentCaptor.forClass(InputStream.class);
  }

  @AfterEach
  void cleanUp() throws Exception {
    closeable.close();
  }

  @Test
  void givenFileNotExists__WhenExecuteTask__ShouldProceedWithoutCheckFileSize()
      throws IOException, FileException {
    // given
    doNothing().when(fileUtil).deleteFile(nameCaptor.capture());
    doNothing().when(fileUtil).copy(inCaptor.capture(), nameCaptor.capture());
    doReturn(null).when(fileUtil).getFilePath(nameCaptor.capture(), anyBoolean());
    when(fileUtil.getSize(nameCaptor.capture())).thenReturn(-1L);
    doReturn(inputStream)
        .when(fileFetchTasklet)
        .wrapInputStream(inCaptor.capture(), msgCaptor.capture());

    // when
    RepeatStatus repeatStatus = fileFetchTasklet.execute(stepContribution, chunkContext);

    // then
    assertThat(nameCaptor.getAllValues()).contains(dump.getFileName());
    assertThat(inCaptor.getAllValues().size()).isEqualTo(2);
    verify(fileUtil, never()).getFilePath(any(), anyBoolean());
    verify(fileUtil, times(1)).getSize(dump.getFileName());
    verify(fileUtil, times(1)).copy(inputStream, dump.getFileName());

    // check batch
    assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
    verify(stepContribution, times(1)).setExitStatus(ExitStatus.COMPLETED);
    verify(stepContribution, times(1)).setExitStatus(ExitStatus.EXECUTING);
    verify(chunkContext, times(1)).setComplete();

    // check logs
    assertThat(logSpy.getLogsByLevel(Level.ERROR).size()).isZero();
    assertThat(logSpy.getLogsAsString(true)).contains("fetching " + dump.getFileName() + "...");
  }

  @Test
  void givenIncompleteFileExists__WhenExecuteTask__ShouldFetchFileAgain()
      throws IOException, FileException {
    // given
    when(fileUtil.getSize(nameCaptor.capture())).thenReturn(100L);
    doReturn(inputStream)
        .when(fileFetchTasklet)
        .wrapInputStream(inCaptor.capture(), msgCaptor.capture());

    // when
    RepeatStatus repeatStatus = fileFetchTasklet.execute(stepContribution, chunkContext);

    // then
    assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
    verify(stepContribution, times(1)).setExitStatus(ExitStatus.COMPLETED);
    verify(stepContribution, times(1)).setExitStatus(ExitStatus.EXECUTING);
    verify(chunkContext, times(1)).setComplete();

    verify(fileUtil, times(1)).deleteFile(dump.getFileName());

    nameCaptor.getAllValues().forEach(name -> assertThat(name).isEqualTo(dump.getFileName()));

    // check logs
    assertThat(logSpy.getLogsByLevel(Level.ERROR).size()).isZero();

    assertThat(logSpy.getLogsAsString(true))
        .contains("found duplicated file: " + dump.getFileName() + ". checking size...");

    assertThat(logSpy.getEvents().get(1).getMessage())
        .isEqualTo("incomplete size. deleting current file...");

    assertThat(logSpy.getEvents().get(2).getMessage())
        .isEqualTo("fetching " + dump.getFileName() + "...");
  }

  //  @Test
  //  void whenExecuteWithCompleteFile__ShouldExitWithoutCopy() {
  //    try {
  //      Files.write(targetPath, RandomString.make(1000).getBytes());
  //      RepeatStatus repeatStatus = fileFetchTasklet.execute(stepContribution, chunkContext);
  //      assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
  //      assertThat(Files.exists(targetPath)).isTrue();
  //      assertThat(Files.size(targetPath)).isEqualTo(Files.size(targetPath));
  //      assertThat(logSpy.getEvents().size()).isEqualTo(2);
  //      assertThat(logSpy.getEvents().get(0).getMessage())
  //          .contains("found duplicated file: ", ". checking size...", dump.getFileName());
  //      assertThat(logSpy.getEvents().get(1).getMessage())
  //          .isEqualTo("file already exists. proceeding...");
  //      assertThat(stepContribution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  //      assertThat(chunkContext.isComplete()).isTrue();
  //    } catch (IOException e) {
  //      fail();
  //    }
  //  }
  //
  //  @Test
  //  void whenExecute__AndDeleteThrows__ThenShouldUpdateStatusAsIntended() throws IOException {
  //    FileFetchTasklet tasklet = Mockito.spy(fileFetchTasklet);
  //    when(tasklet.execute(stepContribution, chunkContext)).thenCallRealMethod();
  //    Mockito.doThrow(FileFetchException.class).when(tasklet).deleteFile(any());
  //    Files.write(targetPath, RandomString.make(500).getBytes());
  //    Throwable t = catchThrowable(() -> tasklet.execute(stepContribution, chunkContext));
  //    assertThat(t).isNotNull().isInstanceOf(FileFetchException.class);
  //    assertThat(stepContribution.getExitStatus()).isEqualTo(ExitStatus.FAILED);
  //    assertThat(chunkContext.isComplete()).isTrue();
  //  }
}
