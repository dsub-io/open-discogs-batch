package io.dsub.discogsdata.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper;

class SystemExitingJobExecutionListenerTest {

  SystemExitingJobExecutionListener listener =
      new SystemExitingJobExecutionListener();

  JobExecution jobExecution;

  @BeforeEach
  void setUp() {
    jobExecution = mock(JobExecution.class);
  }

  @Test
  void whenBeforeJobCalled__ShouldReportToJobExecution() {
    // when
    listener.beforeJob(jobExecution);
    // then
    verify(jobExecution, times(1)).setStatus(BatchStatus.STARTING);
  }

  @Test
  void whenAfterJobCalled__ShouldReportToJobExecution() {

    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

    given(jobExecution.getExitStatus())
        .willReturn(ExitStatus.COMPLETED);

    listener = spy(listener);

    doCallRealMethod().when(listener)
        .afterJob(jobExecution);

    doNothing().when(listener)
        .exit(captor.capture());

    // when
    Thread t = new Thread(() -> listener.afterJob(jobExecution));
    t.start();

    while (t.isAlive()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException ignored) {

      }
    }

    // then
    verify(jobExecution, times(1)).upgradeStatus(BatchStatus.COMPLETED);
    verify(jobExecution, times(1)).getExitStatus();
    assertThat(captor.getValue())
        .isEqualTo(new SimpleJvmExitCodeMapper().intValue(ExitStatus.COMPLETED.getExitCode()));
  }
}