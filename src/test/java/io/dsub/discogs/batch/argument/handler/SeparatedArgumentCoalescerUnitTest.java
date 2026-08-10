package io.dsub.discogs.batch.argument.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

class SeparatedArgumentCoalescerUnitTest {

  private final SeparatedArgumentCoalescer coalescer = new SeparatedArgumentCoalescer();

  @Test
  void coalescesOnlyKnownOptionsThatRequireValues() {
    assertThat(
            coalescer.coalesce(
                new String[] {
                  "--entities", "artist", "--cleanup", "--unknown", "value", "--chunk-size=10"
                }))
        .containsExactly(
            "--entities=artist", "--cleanup", "--unknown", "value", "--chunk-size=10");
  }

  @Test
  void rejectsMissingOrFlagLikeValues() {
    assertThatThrownBy(() -> coalescer.coalesce(new String[] {"--entities"}))
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessage("entities requires a value");
    assertThatThrownBy(
            () -> coalescer.coalesce(new String[] {"--entities", "--cleanup"}))
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessage("entities requires a value");
  }
}
