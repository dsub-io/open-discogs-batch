package io.dsub.discogs.batch.job.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

class AbstractStepConfigTest {

  AbstractStepConfig stepConfig;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() throws InvalidArgumentException {
    stepConfig = Mockito.mock(AbstractStepConfig.class);
    when(stepConfig.executionDecider(any())).thenCallRealMethod();
  }

  @ParameterizedTest
  @ValueSource(strings = {"artist", "release", "master", "label"})
  void whenGetOnKeyExecutionDecider__ShouldReturnValidExecutionDecider(String param)
      throws InvalidArgumentException {
    JobExecutionDecider jobExecutionDecider = stepConfig.executionDecider(param);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);
    JobParameters jobParameters = Mockito.mock(JobParameters.class);
    Map<String, JobParameter> falsyMap = new HashMap<>();
    Map<String, JobParameter> truthyMap = new HashMap<>();
    truthyMap.put(param, new JobParameter("hello"));
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getParameters()).thenReturn(falsyMap);

    FlowExecutionStatus status = jobExecutionDecider.decide(jobExecution, null);
    assertThat(status.getName()).isEqualTo("SKIPPED");

    if (logSpy.countExact(Level.DEBUG) > 0) {
      assertThat(logSpy.getLogsByLevelAsString(Level.DEBUG, true).get(0))
          .contains(param, "skipping");
      logSpy.clear();
    }

    when(jobParameters.getParameters()).thenReturn(truthyMap);
    status = jobExecutionDecider.decide(jobExecution, null);

    assertThat(status.getName()).isEqualTo("COMPLETED");
    if (logSpy.countExact(Level.DEBUG) > 0) {
      assertThat(logSpy.getLogsByLevelAsString(Level.DEBUG, true).get(0))
          .contains(param, "executing");
    }
  }
}
