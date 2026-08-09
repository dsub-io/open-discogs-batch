package io.dsub.discogs.batch.argument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PositiveIntegerParserUnitTest {

  @Test
  void parsesPositiveInteger() {
    assertThat(PositiveIntegerParser.require("max-workers", "4")).isEqualTo(4);
  }

  @Test
  void rejectsZeroNegativeAndOverflowValues() {
    for (String value : new String[] {"0", "-1", "2147483648"}) {
      assertThatThrownBy(() -> PositiveIntegerParser.require("max-workers", value))
          .hasMessage("max-workers must be a positive integer");
    }
  }
}
