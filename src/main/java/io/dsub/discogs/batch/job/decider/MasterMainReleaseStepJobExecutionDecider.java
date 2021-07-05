package io.dsub.discogs.batch.job.decider;

import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

@Slf4j
@RequiredArgsConstructor
// TODO: test!
public class MasterMainReleaseStepJobExecutionDecider implements JobExecutionDecider {

  private static final String MASTER = "master";
  private static final String RELEASE_ITEM = "release";
  private static final String SKIP_MSG = "skipping master main release step.";
  private static final String SKIPPED = "SKIPPED";

  private final EntityIdRegistry idRegistry;

  @Override
  public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
    if (jobExecution.getExitStatus().equals(ExitStatus.FAILED)) {
      log.info("job execution marked as failed. " + SKIP_MSG);
    } else if (stepExecution.getExitStatus().equals(ExitStatus.FAILED)) {
      log.info("step execution marked as failed. " + SKIP_MSG);
    } else if (!jobExecution.getJobParameters().getParameters().containsKey(MASTER)) {
      log.info("master eTag missing. " + SKIP_MSG);
    } else if (!jobExecution.getJobParameters().getParameters().containsKey(RELEASE_ITEM)) {
      log.info("release item eTag missing. " + SKIP_MSG);
    } else if (idRegistry
        .getLongIdCache(EntityIdRegistry.Type.RELEASE)
        .getConcurrentSkipListSet()
        .isEmpty()) {
      log.info("release item identity cache is missing. " + SKIP_MSG);
    } else {
      return FlowExecutionStatus.COMPLETED;
    }
    return new FlowExecutionStatus(SKIPPED);
  }
}
