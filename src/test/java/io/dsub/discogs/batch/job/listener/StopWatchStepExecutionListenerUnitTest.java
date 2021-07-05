package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.util.StopWatch;

class StopWatchStepExecutionListenerUnitTest {

  @Mock
  AtomicLong itemsCounter;

  @InjectMocks
  StopWatchStepExecutionListener listener;

  StepExecution stepExecution;
  StopWatch stopWatch;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    stepExecution = Mockito.mock(StepExecution.class);
    listener = Mockito.spy(listener);
    stopWatch = mock(StopWatch.class);

    doReturn(stopWatch).when(listener).getStopWatch();
    doReturn(0L).when(itemsCounter).get();
    doReturn(500).when(stepExecution).getWriteCount();
    doReturn("testStep").when(stopWatch).getLastTaskName();
    doReturn(10.0).when(stopWatch).getTotalTimeSeconds();
    doReturn(true).when(stopWatch).isRunning();
    reset(itemsCounter);
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
  void whenListenerCalledForAfterStep__ShouldCallStopWatchToStop() {
    // when
    listener.afterStep(stepExecution);

    // then
    verify(stopWatch, times(1)).stop();
  }

  @Test
  void whenListenerCalledForAfterStep__ShouldResetCounterValue() {
    // when
    listener.afterStep(stepExecution);
    // then
    verify(itemsCounter, times(1)).set(0);
  }

  @Test
  void givenTotalTimeSecondsIsZero__WhenAfterStepCalled__ShouldNotLog() {
    // given
    doReturn(0.0).when(stopWatch).getTotalTimeSeconds();

    // when
    listener.afterStep(stepExecution);

    // then
    assertThat(logSpy.countExact(Level.INFO)).isZero();
  }

  @Test
  void givenItemsCounterIsZero__WhenAfterStepCalled__ThenShouldRepostWithStepExecutionValue() {
    try {
      // given
      doReturn(0L).when(itemsCounter).get();

      // when
      listener.beforeStep(stepExecution);
      listener.afterStep(stepExecution);

      assertThat(logSpy.countExact(Level.INFO)).isOne();
      String log = logSpy.getLogsByLevelAsString(Level.INFO, true).get(0);
      assertThat(log).contains("testStep", "500", "50/s", "10");
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void givenItemsCounterIsNonZero__WhenAfterStepCalled__ThenShouldPrioritizeItemCounterValue() {
    try {
      // given
      doReturn(100L).when(itemsCounter).get();

      // when
      listener.beforeStep(stepExecution);
      listener.afterStep(stepExecution);

      verify(itemsCounter, times(1)).set(0);
      assertThat(logSpy.countExact(Level.INFO)).isOne();
      String log = logSpy.getLogsByLevelAsString(Level.INFO, true).get(0);
      assertThat(log).contains("testStep", "100", "10/s", "10");
    } catch (Exception e) {
      fail(e);
    }
  }
}
