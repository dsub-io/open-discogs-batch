package io.dsub.discogs.batch.job.decider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.job.registry.IdCache;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;

class MasterMainReleaseStepJobExecutionDeciderUnitTest {

  private EntityIdRegistry registry;
  private IdCache releaseIds;
  private MasterMainReleaseStepJobExecutionDecider decider;

  @BeforeEach
  void setUp() {
    registry = mock(EntityIdRegistry.class);
    releaseIds = new IdCache(DefaultEntityIdRegistry.Type.RELEASE);
    when(registry.getLongIdCache(DefaultEntityIdRegistry.Type.RELEASE)).thenReturn(releaseIds);
    decider = new MasterMainReleaseStepJobExecutionDecider(registry);
  }

  @ParameterizedTest
  @MethodSource("skippedExecutions")
  void shouldSkipInvalidOrIncompleteExecutions(
      ExitStatus jobStatus, ExitStatus stepStatus, JobParameters parameters) {
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);
    when(jobExecution.getExitStatus()).thenReturn(jobStatus);
    when(stepExecution.getExitStatus()).thenReturn(stepStatus);
    when(jobExecution.getJobParameters()).thenReturn(parameters);

    assertThat(decider.decide(jobExecution, stepExecution).getName()).isEqualTo("SKIPPED");
  }

  @Test
  void shouldSkipWhenReleaseIdentityCacheIsEmpty() {
    assertThat(decider.decide(jobExecution(completeParameters()), stepExecution()).getName())
        .isEqualTo("SKIPPED");
  }

  @Test
  void shouldContinueWhenInputsAndReleaseIdentityCacheExist() {
    releaseIds.add(1);

    assertThat(decider.decide(jobExecution(completeParameters()), stepExecution()).getName())
        .isEqualTo("COMPLETED");
  }

  private static Stream<Arguments> skippedExecutions() {
    return Stream.of(
        Arguments.of(ExitStatus.FAILED, ExitStatus.COMPLETED, completeParameters()),
        Arguments.of(ExitStatus.COMPLETED, ExitStatus.FAILED, completeParameters()),
        Arguments.of(ExitStatus.COMPLETED, ExitStatus.COMPLETED, new JobParameters()),
        Arguments.of(
            ExitStatus.COMPLETED,
            ExitStatus.COMPLETED,
            new JobParametersBuilder().addString("master", "etag").toJobParameters()));
  }

  private static JobParameters completeParameters() {
    return new JobParametersBuilder()
        .addString("master", "master-etag")
        .addString("release", "release-etag")
        .toJobParameters();
  }

  private JobExecution jobExecution(JobParameters parameters) {
    JobExecution execution = mock(JobExecution.class);
    when(execution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
    when(execution.getJobParameters()).thenReturn(parameters);
    return execution;
  }

  private StepExecution stepExecution() {
    StepExecution execution = mock(StepExecution.class);
    when(execution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
    return execution;
  }
}
