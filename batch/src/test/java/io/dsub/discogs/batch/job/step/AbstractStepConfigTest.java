package io.dsub.discogs.batch.job.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.common.exception.InvalidArgumentException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;

class AbstractStepConfigTest {

  AbstractStepConfig stepConfig;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    stepConfig = Mockito.mock(AbstractStepConfig.class);
    when(stepConfig.buildItemWriter(any(), any()))
        .thenCallRealMethod();
    when(stepConfig.getOnKeyExecutionDecider(any()))
        .thenCallRealMethod();
  }

  @Test
  void whenAllItemsGiven__ShouldNotThrowOnBuildItemWriter() {
    DataSource mockDataSource = Mockito.mock(DataSource.class);

    // when
    ItemWriter<?> writer = stepConfig.buildItemWriter("test", mockDataSource);

    // then
    assertThat(writer)
        .isNotNull()
        .isInstanceOf(JdbcBatchItemWriter.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "a"})
  void whenQueryIsNullOrBlank__ShouldThrowOnBuildItemWriter(String param) {
    if (param.length() > 0) {
      param = null;
    }
    String finalParam = param;
    // when
    Throwable t = catchThrowable(
        () -> stepConfig.buildItemWriter(finalParam, Mockito.mock(DataSource.class)));

    // then
    assertThat(t).isInstanceOf(InvalidArgumentException.class)
        .hasMessage("query cannot be null or blank.");
  }

  @Test
  void whenDataSourceIsNull__ShouldThrowOnBuildItemWriter() {
    // when
    Throwable t = catchThrowable(() ->
        stepConfig.buildItemWriter("hello", null));

    // then
    assertThat(t).isInstanceOf(InvalidArgumentException.class)
        .hasMessage("datasource cannot be null.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", ""})
  void whenGetOnKeyExecutionDecider__ShouldThrowIfKeyIsNullOrBlank(String key) {
    String param = key.equals("null") ? null : key;

    // when
    Throwable t = catchThrowable(() -> stepConfig.getOnKeyExecutionDecider(param));

    // then
    assertThat(t).isInstanceOf(InvalidArgumentException.class)
        .hasMessage("key cannot be null or blank.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"artist", "release", "master", "label"})
  void whenGetOnKeyExecutionDecider__ShouldReturnValidExecutionDecider(String param) {
    JobExecutionDecider jobExecutionDecider =
        stepConfig.getOnKeyExecutionDecider(param);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);
    JobParameters jobParameters = Mockito.mock(JobParameters.class);
    Map<String, JobParameter> falsyMap = new HashMap<>();
    Map<String, JobParameter> truthyMap = new HashMap<>();
    truthyMap.put(param, new JobParameter("hello"));
    when(jobExecution.getJobParameters())
        .thenReturn(jobParameters);
    when(jobParameters.getParameters())
        .thenReturn(falsyMap);

    FlowExecutionStatus status = jobExecutionDecider.decide(jobExecution, null);
    assertThat(status.getName()).isEqualTo("SKIPPED");

    if (logSpy.countExact(Level.DEBUG) > 0) {
      assertThat(logSpy.getLogsByLevelAsString(Level.DEBUG, true).get(0))
          .contains(param, "skipping");
      logSpy.clear();
    }

    when(jobParameters.getParameters())
        .thenReturn(truthyMap);
    status = jobExecutionDecider.decide(jobExecution, null);

    assertThat(status.getName()).isEqualTo("COMPLETED");
    if (logSpy.countExact(Level.DEBUG) > 0) {
      assertThat(logSpy.getLogsByLevelAsString(Level.DEBUG, true).get(0))
          .contains(param, "executing");
    }
  }
}