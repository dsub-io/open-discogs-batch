package io.dsub.discogs.batch.util;

import java.io.PrintStream;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;

/**
 * A console progress bar consumer that can be turned on or off.
 */
public class ToggleProgressBarConsumer extends ConsoleProgressBarConsumer {

  private final boolean interactive;
  private boolean print = false;
  private boolean rendered = false;

  /**
   * Constructor to be used with designated {@link PrintStream}.
   *
   * @param out {@link PrintStream} to print progress bar.
   */
  public ToggleProgressBarConsumer(PrintStream out) {
    this(out, TerminalSupport.isInteractive());
  }

  public static ToggleProgressBarConsumer interactive(PrintStream out) {
    return new ToggleProgressBarConsumer(out, true);
  }

  public static ToggleProgressBarConsumer nonInteractive(PrintStream out) {
    return new ToggleProgressBarConsumer(out, false);
  }

  private ToggleProgressBarConsumer(PrintStream out, boolean interactive) {
    super(out, 150);
    this.interactive = interactive;
  }

  /**
   * Regardless of acceptance, the act of print will be judged by either {@link
   * ToggleProgressBarConsumer#print} is on or off.
   */
  @Override
  public void accept(String str) {
    if (this.print) {
      this.rendered = true;
      super.accept(str);
    }
  }

  @Override
  public void close() {
    if (this.rendered) {
      super.close();
    }
  }

  /**
   * Simple on method to activate print.
   */
  public void on() {
    this.print = interactive;
  }

  /**
   * Simple off method to deactivate print.
   */
  public void off() {
    this.print = false;
  }
}
