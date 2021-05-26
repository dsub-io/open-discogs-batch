package io.dsub.discogsdata.batch.util;

import java.io.PrintStream;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;

public class ToggleProgressBarConsumer extends ConsoleProgressBarConsumer {

  private boolean print = false;

  public ToggleProgressBarConsumer(PrintStream out) {
    super(out, 150);
  }

  @Override
  public void accept(String str) {
    if (this.print) {
      super.accept(str);
    }
  }

  public void on() {
    this.print = true;
  }

  public void off() {
    this.print = false;
  }
}
