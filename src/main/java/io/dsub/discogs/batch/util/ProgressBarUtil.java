package io.dsub.discogs.batch.util;

import me.tongfei.progressbar.*;

import java.time.temporal.ChronoUnit;

/**
 * A convenient class to create {@link ProgressBar}. In needs of further customization, use {@link
 * ProgressBarBuilder}.
 */
public class ProgressBarUtil {

    // prevent initialize
    private ProgressBarUtil() {}

    public static ProgressBar get(String taskName, long initialMax) {
        return get(taskName, initialMax, new ConsoleProgressBarConsumer(System.err, 150));
    }

    public static ProgressBar get(String taskName, long initialMax, ProgressBarConsumer consumer) {
        return new ProgressBarBuilder()
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .setUnit("MB", 1048576)
                .setSpeedUnit(ChronoUnit.SECONDS)
                .setUpdateIntervalMillis(100)
                .setTaskName(taskName)
                .setInitialMax(initialMax)
                .setConsumer(consumer)
                .showSpeed()
                .build();
    }
}
