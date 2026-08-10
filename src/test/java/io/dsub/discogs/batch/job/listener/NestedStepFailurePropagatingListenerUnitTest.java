package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;

class NestedStepFailurePropagatingListenerUnitTest {

  @Test
  void propagatesNestedFailedExitEvenWhenBatchStatusIsCompleted() {
    JobExecution jobExecution =
        new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
    StepExecution flow = new StepExecution(1L, "flow", jobExecution);
    StepExecution child = new StepExecution(2L, "child", jobExecution);
    child.setStatus(BatchStatus.COMPLETED);
    child.setExitStatus(ExitStatus.FAILED);
    jobExecution.addStepExecution(flow);
    jobExecution.addStepExecution(child);

    ExitStatus result = new NestedStepFailurePropagatingListener().afterStep(flow);

    assertThat(result.getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(flow.getStatus()).isEqualTo(BatchStatus.FAILED);
  }

  @Test
  void preservesFlowStatusWhenNestedStepsSucceeded() {
    JobExecution jobExecution =
        new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
    StepExecution flow = new StepExecution(1L, "flow", jobExecution);
    StepExecution child = new StepExecution(2L, "child", jobExecution);
    child.setStatus(BatchStatus.COMPLETED);
    child.setExitStatus(ExitStatus.COMPLETED);
    jobExecution.addStepExecution(flow);
    jobExecution.addStepExecution(child);

    ExitStatus result = new NestedStepFailurePropagatingListener().afterStep(flow);

    assertThat(result).isEqualTo(flow.getExitStatus());
    assertThat(flow.getStatus()).isNotEqualTo(BatchStatus.FAILED);
  }

  @Test
  void propagatesNestedBatchFailure() {
    assertNestedFailure(BatchStatus.FAILED, ExitStatus.COMPLETED, false);
  }

  @Test
  void propagatesNestedFailureException() {
    assertNestedFailure(BatchStatus.COMPLETED, ExitStatus.COMPLETED, true);
  }

  private void assertNestedFailure(
      BatchStatus childStatus, ExitStatus childExitStatus, boolean addFailure) {
    JobExecution jobExecution =
        new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
    StepExecution flow = new StepExecution(1L, "flow", jobExecution);
    StepExecution child = new StepExecution(2L, "child", jobExecution);
    child.setStatus(childStatus);
    child.setExitStatus(childExitStatus);
    if (addFailure) {
      child.addFailureException(new IllegalStateException("failure"));
    }
    jobExecution.addStepExecution(flow);
    jobExecution.addStepExecution(child);

    ExitStatus result = new NestedStepFailurePropagatingListener().afterStep(flow);

    assertThat(result.getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(flow.getStatus()).isEqualTo(BatchStatus.FAILED);
  }
}
