package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import io.dsub.discogs.batch.job.progress.ImportProgressSnapshot;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;

class EntityProgressStepExecutionListenerUnitTest {

  @Test
  void startsProgressAndAcceptsPostCommitChunkCallbacks() {
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    when(progressStore.getProgress(6L, EntityType.MASTER))
        .thenReturn(new ImportProgressSnapshot(0, OptionalLong.empty(), Optional.empty()));
    EntityProgressStepExecutionListener listener =
        new EntityProgressStepExecutionListener(
            progressStore, EntityType.MASTER, 6L, 5, true);
    StepExecution stepExecution = stepExecution(BatchStatus.STARTED);

    listener.beforeStep(stepExecution);
    listener.afterChunk(new Chunk<>());

    verify(progressStore).getProgress(6L, EntityType.MASTER);
  }

  @Test
  void incompleteStepDoesNotFinalizeEntityProgress() throws Exception {
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    StepExecution stepExecution = stepExecution(BatchStatus.FAILED);
    EntityProgressStepExecutionListener listener =
        new EntityProgressStepExecutionListener(progressStore, EntityType.ARTIST, 7L, 5, false);

    ExitStatus result = listener.afterStep(stepExecution);

    assertThat(result).isEqualTo(stepExecution.getExitStatus());
    verify(progressStore, never()).completeEntityFromProgress(7L, EntityType.ARTIST, 5);
  }

  @Test
  void completedStepFinalizesCoveredEntityProgress() throws Exception {
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    StepExecution stepExecution = stepExecution(BatchStatus.COMPLETED);
    EntityProgressStepExecutionListener listener =
        new EntityProgressStepExecutionListener(progressStore, EntityType.LABEL, 8L, 10, false);

    ExitStatus result = listener.afterStep(stepExecution);

    assertThat(result).isEqualTo(stepExecution.getExitStatus());
    verify(progressStore).completeEntityFromProgress(8L, EntityType.LABEL, 10);
  }

  @Test
  void deferredCompletionLeavesCoveredProgressOpenForTheFollowingStep() throws Exception {
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    StepExecution stepExecution = stepExecution(BatchStatus.COMPLETED);
    EntityProgressStepExecutionListener listener =
        new EntityProgressStepExecutionListener(
            progressStore,
            EntityType.RELEASE,
            8L,
            10,
            false,
            EntityProgressStepExecutionListener.CompletionPolicy.DEFER);

    ExitStatus result = listener.afterStep(stepExecution);

    assertThat(result).isEqualTo(stepExecution.getExitStatus());
    verify(progressStore, never()).completeEntityFromProgress(8L, EntityType.RELEASE, 10);
  }

  @Test
  void invalidCoverageFailsAnOtherwiseCompletedStep() throws Exception {
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    ImportExecutionException failure = new ImportExecutionException("invalid coverage");
    doThrow(failure)
        .when(progressStore)
        .completeEntityFromProgress(9L, EntityType.RELEASE, 10);
    StepExecution stepExecution = stepExecution(BatchStatus.COMPLETED);
    EntityProgressStepExecutionListener listener =
        new EntityProgressStepExecutionListener(progressStore, EntityType.RELEASE, 9L, 10, false);

    ExitStatus result = listener.afterStep(stepExecution);

    assertThat(result.getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(stepExecution.getFailureExceptions()).containsExactly(failure);
  }

  private StepExecution stepExecution(BatchStatus status) {
    JobExecution jobExecution =
        new JobExecution(1L, new JobInstance(1L, "job"), new JobParameters());
    StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);
    stepExecution.setStatus(status);
    return stepExecution;
  }
}
