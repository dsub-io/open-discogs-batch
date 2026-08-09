package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.FileDeleteException;
import io.dsub.discogs.batch.job.DownloadedFileCleanup;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;

class ClearanceJobExecutionListenerUnitTest {

  @Test
  void cleanupFailureMarksAnOtherwiseCompletedJobAsFailed() throws Exception {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    DownloadedFileCleanup cleanup = mock(DownloadedFileCleanup.class);
    when(cleanup.isEnabled()).thenReturn(true);
    JobParameters parameters = new JobParameters();
    JobExecution execution = new JobExecution(1L, new JobInstance(1L, "job"), parameters);
    execution.setStatus(BatchStatus.COMPLETED);
    FileDeleteException failure =
        new FileDeleteException("fixture cleanup failure", new IllegalStateException());
    doThrow(failure).when(cleanup).cleanup(parameters);

    new ClearanceJobExecutionListener(registry, cleanup).afterJob(execution);

    verify(registry).clearAll();
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(execution.getFailureExceptions()).containsExactly(failure);
  }
}
