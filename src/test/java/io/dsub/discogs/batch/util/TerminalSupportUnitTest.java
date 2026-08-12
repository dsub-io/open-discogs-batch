package io.dsub.discogs.batch.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TerminalSupportUnitTest {

  @Test
  void reportsTheCurrentConsoleState() {
    assertThat(TerminalSupport.isInteractive()).isEqualTo(System.console() != null);
    assertThat(TerminalSupport.isNonInteractive()).isEqualTo(System.console() == null);
  }

  @Test
  void progressConsumerPrintsOnlyWhenInteractiveAndEnabled() {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
      ToggleProgressBarConsumer interactive = ToggleProgressBarConsumer.interactive(stream);
      interactive.accept("hidden");
      interactive.on();
      interactive.accept("visible");
      interactive.off();
      interactive.accept("hidden-again");
      interactive.close();

      ToggleProgressBarConsumer nonInteractive = ToggleProgressBarConsumer.nonInteractive(stream);
      nonInteractive.on();
      nonInteractive.accept("non-interactive");
      nonInteractive.close();
    }

    assertThat(output.toString(StandardCharsets.UTF_8))
        .contains("visible")
        .doesNotContain("hidden")
        .doesNotContain("non-interactive");
  }
}
