package io.dsub.discogs.batch.job.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

class AbstractStepConfigUnitTest {

  AbstractStepConfig stepConfig;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() throws InvalidArgumentException {
    stepConfig = Mockito.mock(AbstractStepConfig.class);
    when(stepConfig.executionDecider(any())).thenCallRealMethod();
  }

  @Test
  void shouldSkipFailedJobBeforeReadingParameters() throws InvalidArgumentException {
    JobExecutionDecider decider = stepConfig.executionDecider("artist");
    JobExecution jobExecution = Mockito.mock(JobExecution.class);
    doReturn(ExitStatus.FAILED).when(jobExecution).getExitStatus();

    assertThat(decider.decide(jobExecution, null).getName()).isEqualTo("SKIPPED");
  }

  @Test
  void supportsEveryNonEmptyEntityCombination() throws InvalidArgumentException {
    String[] entities = {"artist", "label", "master", "release"};
    for (int mask = 1; mask < 1 << entities.length; mask++) {
      JobExecution jobExecution = Mockito.mock(JobExecution.class);
      JobParameters jobParameters = Mockito.mock(JobParameters.class);
      ExitStatus exitStatus = Mockito.mock(ExitStatus.class);
      doReturn("COMPLETED").when(exitStatus).getExitCode();
      doReturn(exitStatus).when(jobExecution).getExitStatus();
      when(jobExecution.getJobParameters()).thenReturn(jobParameters);

      for (int index = 0; index < entities.length; index++) {
        String entity = entities[index];
        boolean selected = (mask & (1 << index)) != 0;
        JobParameter<String> parameter =
            selected ? new JobParameter<>(entity, entity, String.class) : null;
        doReturn(parameter).when(jobParameters).getParameter(entity);

        FlowExecutionStatus status =
            stepConfig.executionDecider(entity).decide(jobExecution, null);

        assertThat(status.getName())
            .as("entity=%s mask=%4s", entity, Integer.toBinaryString(mask))
            .isEqualTo(selected ? "COMPLETED" : "SKIPPED");
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"artist", "release", "master", "label"})
  void whenGetOnKeyExecutionDecider__ShouldReturnValidExecutionDecider(String param)
      throws InvalidArgumentException {

    JobExecutionDecider jobExecutionDecider = stepConfig.executionDecider(param);

    JobExecution jobExecution = Mockito.mock(JobExecution.class);
    JobParameters jobParameters = Mockito.mock(JobParameters.class);

    JobParameter<String> jobParameter =
        new JobParameter<>(param, "hello", String.class);

    ExitStatus exitStatus = Mockito.mock(ExitStatus.class);
    doReturn("COMPLETED").when(exitStatus).getExitCode();
    doReturn(exitStatus).when(jobExecution).getExitStatus();

    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getParameter(param)).thenReturn(null);

    FlowExecutionStatus status = jobExecutionDecider.decide(jobExecution, null);
    assertThat(status.getName()).isEqualTo("SKIPPED");

    if (logSpy.countExact(Level.DEBUG) > 0) {
      assertThat(logSpy.getLogsByLevelAsString(Level.DEBUG, true).get(0))
          .contains(param, "skipping");
      logSpy.clear();
    }

    doReturn(jobParameter).when(jobParameters).getParameter(param);
    status = jobExecutionDecider.decide(jobExecution, null);

    assertThat(status.getName()).isEqualTo("COMPLETED");
    if (logSpy.countExact(Level.DEBUG) > 0) {
      assertThat(logSpy.getLogsByLevelAsString(Level.DEBUG, true).get(0))
          .contains(param, "executing");
    }
  }
}
