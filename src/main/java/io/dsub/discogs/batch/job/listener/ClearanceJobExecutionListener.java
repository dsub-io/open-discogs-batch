package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.exception.FileDeleteException;
import io.dsub.discogs.batch.job.DownloadedFileCleanup;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

@Slf4j
@RequiredArgsConstructor
public class ClearanceJobExecutionListener implements JobExecutionListener {

  private final EntityIdRegistry registry;
  private final DownloadedFileCleanup downloadedFileCleanup;

  @Override
  public void beforeJob(JobExecution jobExecution) {
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    clearCache();
    clearFiles(jobExecution);
  }

  private void clearFiles(JobExecution jobExecution) {
    if (!downloadedFileCleanup.isEnabled()) {
      log.info("cleanup option not applied. keeping downloaded files.");
      return;
    }
    if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
      log.info("job did not complete successfully. keeping downloaded files for retry.");
      return;
    }
    try {
      downloadedFileCleanup.cleanup(jobExecution.getJobParameters());
    } catch (FileDeleteException e) {
      log.error("failed to remove downloaded files", e);
      jobExecution.addFailureException(e);
      jobExecution.setStatus(BatchStatus.FAILED);
    }
  }

  private void clearCache() {
    registry.clearAll();
    log.info("cache cleared");
  }
}
