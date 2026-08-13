package io.dsub.discogs.batch.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DiscogsStringNormalizerUnitTest {

  private static final List<Integer> WHITE_SPACE =
      List.of(
          0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x0020, 0x0085, 0x00A0, 0x1680,
          0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008,
          0x2009, 0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000);

  @Test
  void ownsTheExactCrossLanguageWhiteSpaceSet() {
    List<Integer> actual =
        IntStream.rangeClosed(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
            .filter(DiscogsStringNormalizer::isWhiteSpace)
            .boxed()
            .toList();
    assertThat(actual).containsExactlyElementsOf(WHITE_SPACE);
  }

  @Test
  void trimsBoundariesAndConvertsWhiteSpaceOnlyValuesToNull() {
    for (Integer codePoint : WHITE_SPACE) {
      String space = Character.toString(codePoint);
      assertThat(DiscogsStringNormalizer.normalizeNullable(space + "Producer" + space))
          .isEqualTo("Producer");
      assertThat(DiscogsStringNormalizer.normalizeNullable(space)).isNull();
    }
    assertThat(DiscogsStringNormalizer.normalizeNullable("A\u00A0B")).isEqualTo("A\u00A0B");
  }
}
