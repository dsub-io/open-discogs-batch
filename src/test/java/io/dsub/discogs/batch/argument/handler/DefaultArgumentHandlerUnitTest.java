package io.dsub.discogs.batch.argument.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class DefaultArgumentHandlerUnitTest {

  private final DefaultArgumentHandler handler = new DefaultArgumentHandler(Map.of());

  @Test
  void normalizesCommaSeparatedValuesFlagsAndAlreadyNormalizedArguments() {
    assertThat(
            handler
                .normalizeArguments(
                    new DefaultApplicationArguments(
                        "--entities=artist,label", "cleanup", "unknown"))
                .getSourceArgs())
        .containsExactly("--entities=artist", "--entities=label", "--cleanup", "--unknown");

    assertThat(
            handler.normalizeArguments(new DefaultApplicationArguments("unknown=one,two"))
                .getSourceArgs())
        .containsExactly("--unknown=one,two");

    assertThat(handler.addFlags(new String[] {"entities=artist", "--cleanup"}))
        .containsExactly("--entities=artist", "--cleanup");
  }
}
