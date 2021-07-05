package io.dsub.discogs.batch.job.step;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

@Slf4j
public abstract class AbstractStepConfig {

  protected static final String CHUNK = "#{jobParameters['chunkSize']}";
  protected static final String ANY = "*";
  protected static final String FAILED = "FAILED";
  protected static final String SKIPPED = "SKIPPED";
  protected static final String ARTIST = "artist";
  protected static final String LABEL = "label";
  protected static final String MASTER = "master";
  protected static final String RELEASE = "release";

  protected JobExecutionDecider executionDecider(String etagKey) {
    return (jobExecution, stepExecution) -> {
      if (jobExecution.getExitStatus().getExitCode().equals("FAILED")) {
        log.info("job execution marked as failed. skipping {} step", etagKey);
        return new FlowExecutionStatus(SKIPPED);
      }
      if (jobExecution.getJobParameters().getParameters().containsKey(etagKey)) {
        log.info("{} eTag found. executing {} step.", etagKey, etagKey);
        return FlowExecutionStatus.COMPLETED;
      }
      log.info("{} eTag not found. skipping {} step.", etagKey, etagKey);
      return new FlowExecutionStatus(SKIPPED);
    };
  }
}
