package io.dsub.discogsdata.batch.util;

import java.time.temporal.ChronoUnit;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarStyle;

public class ProgressBarUtil {

  private ProgressBarUtil() {
  }

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
