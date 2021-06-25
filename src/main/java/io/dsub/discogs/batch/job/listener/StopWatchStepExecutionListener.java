package io.dsub.discogs.batch.job.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.util.NumberUtils;
import org.springframework.util.StopWatch;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class StopWatchStepExecutionListener implements StepExecutionListener {

    private final AtomicLong itemsCounter;
    private StopWatch stopWatch;

    public StopWatchStepExecutionListener(final AtomicLong itemsCounter) {
        this.itemsCounter = itemsCounter;
        this.stopWatch = new StopWatch();
        this.stopWatch.setKeepTaskList(false);
        this.init();
    }

    private void init() {
        this.stopWatch = getStopWatch();
        this.itemsCounter.set(0);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepExecution.setStatus(BatchStatus.STARTING);
        getStopWatch().start(stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        printStepDetails(itemsCounter.get() == 0 ? stepExecution.getWriteCount() : itemsCounter.get());
        stepExecution.setStatus(BatchStatus.COMPLETED);
        this.init();
        return ExitStatus.COMPLETED;
    }

    /**
     * reports step execution details as log.
     *
     * @param writeCount items has been written during step
     */
    private void printStepDetails(long writeCount) {
        int seconds = getTotalTimeSeconds();

        if (seconds == 0) { // nothing to report...
            return;
        }

        long itemsPerSecond = writeCount / seconds;

        String timeTookSeconds = String.valueOf(seconds);
        String itemsProcPerSec = itemsPerSecond + "/s";
        String taskName = getStopWatch().getLastTaskName();

        log.info("task {} took {} seconds and updated {} items. processed items per second: {}",
                taskName,
                timeTookSeconds,
                writeCount,
                itemsProcPerSec);
    }

    protected StopWatch getStopWatch() {
        return stopWatch;
    }

    protected int getTotalTimeSeconds() {
        StopWatch stopWatch = getStopWatch();
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        }
        return NumberUtils.convertNumberToTargetClass(stopWatch.getTotalTimeSeconds(), Integer.class);
    }
}
