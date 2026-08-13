package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.progress.ImportProgressReporter;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

public class EntityProgressStepExecutionListener
    implements StepExecutionListener, ChunkListener<Void, Void> {

  private final ImportProgressStore progressStore;
  private final EntityType entityType;
  private final long runId;
  private final int chunkSize;
  private final CompletionPolicy completionPolicy;
  private final ImportProgressReporter progressReporter;

  public EntityProgressStepExecutionListener(
      ImportProgressStore progressStore,
      EntityType entityType,
      long runId,
      int chunkSize,
      boolean resumed) {
    this(progressStore, entityType, runId, chunkSize, resumed, CompletionPolicy.FINALIZE);
  }

  public EntityProgressStepExecutionListener(
      ImportProgressStore progressStore,
      EntityType entityType,
      long runId,
      int chunkSize,
      boolean resumed,
      CompletionPolicy completionPolicy) {
    this.progressStore = progressStore;
    this.entityType = entityType;
    this.runId = runId;
    this.chunkSize = chunkSize;
    this.completionPolicy = completionPolicy;
    this.progressReporter =
        new ImportProgressReporter(progressStore, entityType, runId, resumed);
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    progressReporter.start();
  }

  @Override
  public void afterChunk(Chunk<Void> chunk) {
    progressReporter.reportIfDue();
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
      progressReporter.finish(false);
      return stepExecution.getExitStatus();
    }
    if (completionPolicy == CompletionPolicy.DEFER) {
      return stepExecution.getExitStatus();
    }
    try {
      progressStore.completeEntityFromProgress(runId, entityType, chunkSize);
      progressReporter.finish(true);
      return stepExecution.getExitStatus();
    } catch (Exception exception) {
      stepExecution.addFailureException(exception);
      stepExecution.setStatus(BatchStatus.FAILED);
      progressReporter.finish(false);
      return ExitStatus.FAILED.addExitDescription(exception);
    }
  }

  public enum CompletionPolicy {
    FINALIZE,
    DEFER
  }
}
