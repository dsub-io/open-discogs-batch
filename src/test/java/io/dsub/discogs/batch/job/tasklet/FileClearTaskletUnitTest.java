package io.dsub.discogs.batch.job.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

class FileClearTaskletUnitTest {

  final JobExecution jobExecution =
      new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
  final StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);
  final ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
  final StepContribution stepContribution = new StepContribution(stepExecution);

  @InjectMocks
  FileClearTasklet fileClearTasklet;

  @Mock
  FileUtil fileUtil;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void givenFilesAreTemporary__WhenTaskExecutes__ShouldCallClearAll() {
    try {
      // given
      given(fileUtil.isTemporary()).willReturn(true);

      // when
      fileClearTasklet.execute(stepContribution, chunkContext);

      // then
      verify(fileUtil, times(1)).clearAll();
    } catch (FileException e) {
      fail(e);
    }
  }

  @Test
  void givenFilesAreMounted__WhenTaskExecutes__ShouldNotCallClearAll() {
    try {
      // given
      given(fileUtil.isTemporary()).willReturn(false);

      // when
      fileClearTasklet.execute(stepContribution, chunkContext);

      // then
      verify(fileUtil, never()).clearAll();
    } catch (FileException e) {
      fail(e);
    }
  }

  @Test
  void givenFilesAreMounted__WhenTaskExecutes__ShouldMarkedAsComplete() {
    // given
    given(fileUtil.isTemporary()).willReturn(false);

    // when
    fileClearTasklet.execute(stepContribution, chunkContext);

    // then
    assertThat(chunkContext.isComplete()).isTrue();
    assertThat(stepContribution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
  }

  @Test
  void givenClearAllThrows__WhenTaskExecutes__WillMarkAsComplete() {
    try {
      // given
      given(fileUtil.isTemporary()).willReturn(true);
      willThrow(new FileException("FAIL")).given(fileUtil).clearAll();

      // when
      fileClearTasklet.execute(stepContribution, chunkContext);

      // then
      assertThat(chunkContext.isComplete()).isTrue();
      assertThat(stepContribution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
    } catch (FileException e) {
      fail(e);
    }
  }
}
