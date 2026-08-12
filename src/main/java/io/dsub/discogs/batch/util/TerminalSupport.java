package io.dsub.discogs.batch.util;

import java.util.Objects;

/** Describes whether the process is attached to an interactive system console. */
public final class TerminalSupport {

  private TerminalSupport() {
  }

  public static boolean isInteractive() {
    return Objects.nonNull(System.console());
  }

  public static boolean isNonInteractive() {
    return Objects.isNull(System.console());
  }
}
