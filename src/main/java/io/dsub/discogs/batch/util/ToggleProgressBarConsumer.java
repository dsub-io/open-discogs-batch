package io.dsub.discogs.batch.util;

import java.io.PrintStream;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;

/**
 * A console progress bar consumer that can be turned on or off.
 */
public class ToggleProgressBarConsumer extends ConsoleProgressBarConsumer {

  private boolean print = false;

  /**
   * Constructor to be used with designated {@link PrintStream}.
   *
   * @param out {@link PrintStream} to print progress bar.
   */
  public ToggleProgressBarConsumer(PrintStream out) {
    super(out, 150);
  }

  /**
   * Regardless of acceptance, the act of print will be judged by either {@link
   * ToggleProgressBarConsumer#print} is on or off.
   */
  @Override
  public void accept(String str) {
    if (this.print) {
      super.accept(str);
    }
  }

  /**
   * Simple on method to activate print.
   */
  public void on() {
    this.print = true;
  }

  /**
   * Simple off method to deactivate print.
   */
  public void off() {
    this.print = false;
  }
}
