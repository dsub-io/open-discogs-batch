package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;

class ClearanceJobExecutionListenerUnitTest {

  @Test
  void completionClearsOnlyTheProcessLocalRegistry() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    JobParameters parameters = new JobParameters();
    JobExecution execution = new JobExecution(1L, new JobInstance(1L, "job"), parameters);
    execution.setStatus(BatchStatus.COMPLETED);

    new ClearanceJobExecutionListener(registry).afterJob(execution);

    verify(registry).clearAll();
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(execution.getFailureExceptions()).isEmpty();
  }
}
