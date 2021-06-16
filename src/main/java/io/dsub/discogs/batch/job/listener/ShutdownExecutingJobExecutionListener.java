package io.dsub.discogs.batch.job.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShutdownExecutingJobExecutionListener implements JobExecutionListener {

  /* no op */
  @Override
  public void beforeJob(JobExecution jobExecution) {}

  @Override
  public void afterJob(JobExecution jobExecution) {
    log.info("calling shutdown...");
    Runtime current = Runtime.getRuntime();
    current.addShutdownHook(
        new Thread() {
          @Override
          public void run() {
            super.run();
          }
        });
  }
}
