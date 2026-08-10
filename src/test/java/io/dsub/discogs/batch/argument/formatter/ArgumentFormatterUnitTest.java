package io.dsub.discogs.batch.argument.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArgumentFormatterUnitTest {

  @Test
  void flagFormatterHandlesNullBlankAndPrefixedArguments() {
    FlagRemovingArgumentFormatter formatter = new FlagRemovingArgumentFormatter();

    assertThat(formatter.format(null)).isNull();
    assertThat(formatter.format(new String[] {null, "", "  ", "--entities=artist", "cleanup"}))
        .containsExactly(null, "", "  ", "entities=artist", "cleanup");
  }

  @Test
  void nameFormatterNormalizesKnownAliasesAndPreservesUnknownArguments() {
    ArgumentNameFormatter formatter = new ArgumentNameFormatter();

    assertThat(
            formatter.format(
                new String[] {
                  "artists=artist", "cleanup=ignored", "unknown=value", "unknowns", "entities="
                }))
        .containsExactly(
            "artists=artist", "cleanup", "unknown=value", "unknowns", "entities=");
  }
}
