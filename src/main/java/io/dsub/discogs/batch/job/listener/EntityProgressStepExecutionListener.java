package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

@RequiredArgsConstructor
public class EntityProgressStepExecutionListener implements StepExecutionListener {

  private final ImportProgressStore progressStore;
  private final EntityType entityType;
  private final long runId;
  private final int chunkSize;

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
      return stepExecution.getExitStatus();
    }
    try {
      progressStore.completeEntityFromProgress(runId, entityType, chunkSize);
      return stepExecution.getExitStatus();
    } catch (Exception exception) {
      stepExecution.addFailureException(exception);
      stepExecution.setStatus(BatchStatus.FAILED);
      return ExitStatus.FAILED.addExitDescription(exception);
    }
  }
}
