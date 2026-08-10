package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

@Slf4j
@RequiredArgsConstructor
public class ClearanceJobExecutionListener implements JobExecutionListener {

  private final EntityIdRegistry registry;

  @Override
  public void beforeJob(JobExecution jobExecution) {
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    clearCache();
  }

  private void clearCache() {
    registry.clearAll();
    log.info("cache cleared");
  }
}
