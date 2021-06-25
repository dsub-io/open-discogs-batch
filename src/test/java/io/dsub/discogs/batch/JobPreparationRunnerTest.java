package io.dsub.discogs.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.job.JobParameterResolver;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class JobPreparationRunnerTest {

  @Mock ApplicationArguments args;
  @Mock JobParameterResolver jobParameterResolver;
  @Mock JobParametersConverter jobParametersConverter;
  @Mock HikariDataSource dataSource;
  @Mock ThreadPoolTaskExecutor taskExecutor;
  @Mock ConfigurableApplicationContext ctx;
  @InjectMocks JobPreparationRunner runner;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void givenProcessorCnt__ShouldSetMaxPoolSizeGreaterByThree() {
    // given
    int procCnt = Runtime.getRuntime().availableProcessors();
    when(taskExecutor.getMaxPoolSize()).thenReturn(procCnt);

    // when
    runner.run(args);

    // then
    verify(dataSource, times(1)).setMaximumPoolSize(procCnt + 3);
  }

  @Test
  void givenApplicationArgument__WhenGetDiscogsJobParameters__ShouldCallResolverAndConverter() {
    Properties properties = mock(Properties.class);
    JobParameters jobParameters = mock(JobParameters.class);
    willReturn(properties).given(jobParameterResolver).resolve(args);
    willReturn(jobParameters).given(jobParametersConverter).getJobParameters(properties);

    // when
    JobParameters result = runner.getDiscogsJobParameters(args);

    // then
    assertAll(
        () -> verify(jobParameterResolver, times(1)).resolve(args),
        () -> verify(jobParametersConverter, times(1)).getJobParameters(properties),
        () -> assertThat(result).isEqualTo(jobParameters));
  }
}
