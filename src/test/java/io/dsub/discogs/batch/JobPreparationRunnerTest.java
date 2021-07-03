package io.dsub.discogs.batch;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.dump.DumpDependencyResolver;
import io.dsub.discogs.batch.job.JobParameterResolver;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

class JobPreparationRunnerTest {

  @Mock ApplicationArguments args;
  @Mock JobParameterResolver jobParameterResolver;
  @Mock JobParametersConverter jobParametersConverter;
  @Mock HikariDataSource dataSource;
  @Mock ThreadPoolTaskExecutor taskExecutor;
  @InjectMocks JobPreparationRunner runner;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

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

  @Nested
  class ContextTest {

    ApplicationContextRunner ctx;
    JobParameterResolver jobParameterResolver;
    DumpDependencyResolver dumpDependencyResolver;
    JobParametersConverter jobParametersConverter;
    ApplicationArguments arguments;
    DataSource dataSource;

    @BeforeEach
    void setUp() {
      jobParameterResolver = Mockito.mock(JobParameterResolver.class);
      arguments = Mockito.mock(ApplicationArguments.class);
      dumpDependencyResolver = Mockito.mock(DumpDependencyResolver.class);
      jobParametersConverter = Mockito.mock(JobParametersConverter.class);
      dataSource = Mockito.mock(DataSource.class);
      ctx = new ApplicationContextRunner()
              .withUserConfiguration(JobPreparationRunner.class)
              .withBean(JobParameterResolver.class, () -> jobParameterResolver)
              .withBean(DumpDependencyResolver.class, () -> dumpDependencyResolver)
              .withBean(JobParametersConverter.class, () -> jobParametersConverter)
              .withBean(ApplicationArguments.class, () -> arguments)
              .withBean(ThreadPoolTaskExecutor.class, () -> taskExecutor);
    }

    @Test
    void givenContext__WhenInitialized__ShouldHaveCountDownLatchBean() {
      ctx = ctx.withBean(DataSource.class, () -> dataSource);

      // when
      ctx.run(it -> assertThat(it)
              .hasSingleBean(JobParameters.class)
              .hasSingleBean(CountDownLatch.class));
    }
  }
}
