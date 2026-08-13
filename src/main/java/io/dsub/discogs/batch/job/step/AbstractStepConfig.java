package io.dsub.discogs.batch.job.step;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import java.util.OptionalLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

@Slf4j
public abstract class AbstractStepConfig {

  protected static final String CHUNK = "#{jobParameters['chunkSize']}";
  protected static final String RUN_ID = "#{jobParameters['import.runId']}";
  protected static final String RESUMED = "#{jobParameters['import.resumed']}";
  protected static final String ANY = "*";
  protected static final String FAILED = "FAILED";
  protected static final String SKIPPED = "SKIPPED";
  protected static final String ARTIST = "artist";
  protected static final String LABEL = "label";
  protected static final String MASTER = "master";
  protected static final String RELEASE = "release";
  protected static final int TRACKED_CHUNKS_PER_TRANSACTION = 1;

  protected JobExecutionDecider executionDecider(String etagKey) {
    return (jobExecution, stepExecution) -> {
      if (jobExecution.getExitStatus().getExitCode().equals("FAILED")) {
        log.info("job execution marked as failed. skipping {} step", etagKey);
        return new FlowExecutionStatus(SKIPPED);
      }
      if (jobExecution.getJobParameters().getParameter(etagKey) != null) {
        log.info("{} eTag found. executing {} step.", etagKey, etagKey);
        return FlowExecutionStatus.COMPLETED;
      }
      log.info("{} eTag not found. skipping {} step.", etagKey, etagKey);
      return new FlowExecutionStatus(SKIPPED);
    };
  }

  protected JobExecutionDecider executionDecider(
      String etagKey,
      EntityType entityType,
      ImportProgressStore progressStore,
      long runId,
      int chunkSize,
      boolean resumed) {
    JobExecutionDecider selectedEntityDecider = executionDecider(etagKey);
    return (jobExecution, stepExecution) -> {
      FlowExecutionStatus selected =
          selectedEntityDecider.decide(jobExecution, stepExecution);
      if (selected.equals(new FlowExecutionStatus(SKIPPED))) {
        return selected;
      }
      OptionalLong completedItems =
          progressStore.completedEntityItems(runId, entityType, chunkSize, resumed);
      if (completedItems.isPresent()) {
        log.info(
            "{} source already completed with {} items. skipping entity flow.",
            etagKey,
            completedItems.getAsLong());
        return new FlowExecutionStatus(SKIPPED);
      }
      return selected;
    };
  }
}
