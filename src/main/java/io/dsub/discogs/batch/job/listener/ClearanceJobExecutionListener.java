package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

@Slf4j
@RequiredArgsConstructor
public class ClearanceJobExecutionListener implements JobExecutionListener {

  private final EntityIdRegistry registry;
  private final FileUtil fileUtil;

  @Override
  public void beforeJob(JobExecution jobExecution) {
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    clearCache();
    clearFiles();
  }

  private void clearFiles() {
    if (fileUtil.isTemporary()) {
      try {
        fileUtil.clearAll();
      } catch (FileException e) {
        log.error("failed to clear application directory", e);
      }
    } else {
      log.info("mount option applied. skipping file deletion");
    }
  }

  private void clearCache() {
    registry.clearAll();
    log.info("cache cleared");
  }
}
