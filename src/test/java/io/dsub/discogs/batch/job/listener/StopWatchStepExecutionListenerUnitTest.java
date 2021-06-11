package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.util.StopWatch;

class StopWatchStepExecutionListenerUnitTest {

  StopWatchStepExecutionListener listener;
  StepExecution stepExecution;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    listener = new StopWatchStepExecutionListener();
    stepExecution = Mockito.mock(StepExecution.class);
  }

  @Test
  void whenBeforeAndAfterStep__ShouldReportToStepExecution() {
    // when
    listener.beforeStep(stepExecution);
    // then
    verify(stepExecution, times(1)).setStatus(BatchStatus.STARTING);

    // when
    listener.afterStep(stepExecution);

    // then
    verify(stepExecution, times(1)).setStatus(BatchStatus.COMPLETED);
  }

  @Test
  void whenListenerCalledForAfterStep__ShouldReturnExitStatus() {
    // when
    ExitStatus exitStatus = listener.afterStep(stepExecution);

    // then
    assertThat(exitStatus).isNotNull().isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  void whenListenerCalledForAfterStep__ShouldPrintValidResult() {

    try {
      given(stepExecution.getWriteCount()).willReturn(100);
      given(stepExecution.getStepName()).willReturn("testStep");

      StopWatch stopWatch = new StopWatch();
      stopWatch = Mockito.spy(stopWatch);

      given(stopWatch.getTotalTimeSeconds()).willReturn(10.0);

      Field stopWatchField = listener.getClass().getDeclaredField("stopWatch");
      stopWatchField.setAccessible(true);
      stopWatchField.set(listener, stopWatch);

      // when
      listener.beforeStep(stepExecution);
      listener.afterStep(stepExecution);

      assertThat(logSpy.countExact(Level.INFO)).isOne();
      String log = logSpy.getLogsByLevelAsString(Level.INFO, true).get(0);
      assertThat(log).contains("testStep", "100", "10/s", "10.0");
    } catch (Exception e) {
      fail(e);
    }
  }
}
