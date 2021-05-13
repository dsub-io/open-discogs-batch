package io.dsub.discogsdata.batch.util;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.time.temporal.ChronoUnit;

public class ProgressBarUtil {
  private ProgressBarUtil(){};
  public static ProgressBar get(String taskName, long initialMax) {
    return new ProgressBarBuilder()
        .setStyle(ProgressBarStyle.ASCII)
        .setUnit("KB", 1024)
        .setSpeedUnit(ChronoUnit.HOURS)
        .setUpdateIntervalMillis(100)
        .setTaskName(taskName)
        .setInitialMax(initialMax)
        .showSpeed()
        .build();
  }
}
