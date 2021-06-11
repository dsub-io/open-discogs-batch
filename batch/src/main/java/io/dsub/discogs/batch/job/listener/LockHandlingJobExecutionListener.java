package io.dsub.discogs.batch.job.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * JobExecutionListener implementation to support batch process. The actual disabling and enabling
 * job execution is based on its implementation.
 */
public abstract class LockHandlingJobExecutionListener implements JobExecutionListener {

  /** Temporarily disable the constraint check or its equivalent from target database. */
  protected abstract void disableConstraints();

  /** Re-enable the constraint check or its equivalent from target database. */
  protected abstract void enableConstraints();

  @Override
  public void beforeJob(JobExecution jobExecution) {
    disableConstraints();
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    enableConstraints();
  }
}
