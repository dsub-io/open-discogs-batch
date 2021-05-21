package io.dsub.discogsdata.batch.job.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.util.NumberUtils;
import org.springframework.util.StopWatch;

@Slf4j
public class DefaultDiscogsStepExecutionListener implements StepExecutionListener {

  StopWatch stopWatch = new StopWatch();

  @Override
  public void beforeStep(StepExecution stepExecution) {
    stepExecution.setStatus(BatchStatus.STARTING);
    stopWatch.start(stepExecution.getStepName());
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    printStepDetails(stepExecution.getWriteCount());
    stepExecution.setStatus(BatchStatus.COMPLETED);
    return ExitStatus.COMPLETED;
  }

  /**
   * reports step execution details as log.
   *
   * @param writeCount items has been written during step
   */
  private void printStepDetails(int writeCount) {
    if (stopWatch.isRunning()) {
      stopWatch.stop();
    }

    int seconds =
        NumberUtils.convertNumberToTargetClass(stopWatch.getTotalTimeSeconds(), Integer.class);
    int itemsPerSecond = writeCount / seconds;
    String timeTook = stopWatch.prettyPrint();
    String itemsWritten =
        "write count: " + writeCount; // this may depends on how item writer reports.
    String itemsProcPerSec = "(" + itemsPerSecond + "/sec)";

    log.info(String.join(" ", timeTook, itemsWritten, itemsProcPerSec));
  }
}
