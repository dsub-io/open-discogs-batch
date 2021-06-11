package io.dsub.discogs.batch.job.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.util.NumberUtils;
import org.springframework.util.StopWatch;

@Slf4j
public class StopWatchStepExecutionListener implements StepExecutionListener {

  private StopWatch stopWatch;

  public StopWatchStepExecutionListener() {
    this.init();
  }

  private void init() {
    this.stopWatch = new StopWatch();
    this.stopWatch.setKeepTaskList(false);
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    stepExecution.setStatus(BatchStatus.STARTING);
    stopWatch.start(stepExecution.getStepName());
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    printStepDetails(stepExecution.getWriteCount());
    stepExecution.setStatus(BatchStatus.COMPLETED);
    this.init();
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

    if (seconds == 0) { // nothing to report...
      return;
    }

    int itemsPerSecond = writeCount / seconds;

    String timeTookSeconds = String.valueOf(stopWatch.getTotalTimeSeconds());

    String itemsProcPerSec = itemsPerSecond + "/s";

    log.info(
        "task {} took {} seconds and written {} items. processed items per second: {}",
        stopWatch.getLastTaskName(),
        timeTookSeconds,
        writeCount,
        itemsProcPerSec);
  }
}
