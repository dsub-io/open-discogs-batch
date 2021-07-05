package io.dsub.discogs.batch.job.listener;

import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

@Slf4j
@RequiredArgsConstructor
public class ExitSignalJobExecutionListener implements JobExecutionListener {

  private final CountDownLatch exitLatch;

  /* no op */
  @Override
  public void beforeJob(JobExecution jobExecution) {
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    log.info("signal exit...");
    exitLatch.countDown();
  }
}
