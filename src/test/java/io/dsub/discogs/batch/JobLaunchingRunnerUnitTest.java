package io.dsub.discogs.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class JobLaunchingRunnerUnitTest {

  @Mock
  Job job;
  @Mock
  JobParameters discogsJobParameters;
  @Mock
  JobOperator jobOperator;
  @Mock
  ApplicationArguments args;
  @Mock
  JobExecution jobExecution;
  @Mock
  ExitStatus exitStatus;
  @Mock
  ConfigurableApplicationContext context;
  @Mock
  CountDownLatch countDownLatch;
  @InjectMocks
  JobLaunchingRunner runner;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    doReturn(jobExecution).when(jobOperator).start(job, discogsJobParameters);
    doReturn(false).when(exitStatus).isRunning();
    doReturn(false).when(jobExecution).isRunning();
    doReturn(exitStatus).when(jobExecution).getExitStatus();
    doReturn(Collections.emptyList()).when(jobExecution).getFailureExceptions();
    runner = spy(runner);
  }

  @Test
  void whenRunCalled__ShouldCallJobOperatorWithJobAndJobParameters() throws Exception {
    // when
    runner.run(args);

    // then
    verify(jobOperator, times(1)).start(job, discogsJobParameters);
  }

  @Test
  void whenRunCalled__ShouldCallSpringApplicationExit() {
    try (MockedStatic<SpringApplication> app = Mockito.mockStatic(SpringApplication.class)) {
      // given
      ExitCodeGenerator exitCodeGenerator = runner.getExitCodeGenerator(jobExecution);
      doReturn(exitCodeGenerator).when(runner).getExitCodeGenerator(jobExecution);

      // when
      runner.run(args);

      // then
      app.verify(() -> SpringApplication.exit(context, exitCodeGenerator), times(1));

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void whenRunCalled__ShouldWaitForCountDownLatch() throws Exception {
    runner.run(args);
    verify(countDownLatch, times(1)).await();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void givenJobExecutionHasFailureExceptions__WhenExitCodeGeneratorGetExitCode__ShouldReturnProperExitCode(
      int errorsCnt) {
    // given
    List<Throwable> mockList = spy(new ArrayList<>());
    doReturn(errorsCnt).when(mockList).size();
    doReturn(mockList).when(jobExecution).getFailureExceptions();

    // when
    int exitCode = runner.getExitCodeGenerator(jobExecution).getExitCode();

    // then
    assertThat(exitCode).isEqualTo(errorsCnt > 0 ? 1 : 0);
  }
}
