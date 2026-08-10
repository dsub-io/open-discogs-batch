package io.dsub.discogs.batch.job.listener;

import java.util.Objects;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

/** Makes a FlowStep fail when one of its nested steps failed. */
public class NestedStepFailurePropagatingListener implements StepExecutionListener {

  @Override
  public ExitStatus afterStep(StepExecution flowStepExecution) {
    boolean nestedFailure =
        flowStepExecution.getJobExecution().getStepExecutions().stream()
            .filter(
                stepExecution ->
                    !Objects.equals(stepExecution.getId(), flowStepExecution.getId()))
            .anyMatch(
                stepExecution ->
                    stepExecution.getStatus() == BatchStatus.FAILED
                        || ExitStatus.FAILED
                            .getExitCode()
                            .equals(stepExecution.getExitStatus().getExitCode())
                        || !stepExecution.getFailureExceptions().isEmpty());
    if (!nestedFailure) {
      return flowStepExecution.getExitStatus();
    }
    flowStepExecution.setStatus(BatchStatus.FAILED);
    return ExitStatus.FAILED.addExitDescription("a nested import step failed");
  }
}
