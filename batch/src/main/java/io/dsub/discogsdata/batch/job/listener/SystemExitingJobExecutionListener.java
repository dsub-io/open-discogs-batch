package io.dsub.discogsdata.batch.job.listener;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper;

public class SystemExitingJobExecutionListener implements JobExecutionListener {

  @Override
  public void beforeJob(JobExecution jobExecution) {
    jobExecution.setStatus(BatchStatus.STARTING);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    jobExecution.upgradeStatus(BatchStatus.COMPLETED);
    this.exit(
        new SimpleJvmExitCodeMapper()
            .intValue(jobExecution.getExitStatus().getExitCode()));
  }

  public void exit(int exitCode) {
    System.exit(exitCode);
  }
}