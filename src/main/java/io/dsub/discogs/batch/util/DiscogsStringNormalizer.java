package io.dsub.discogs.batch.util;

/** Applies the model-owned Unicode White_Space boundary contract. */
public final class DiscogsStringNormalizer {

  private DiscogsStringNormalizer() {
  }

  public static String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    int start = 0;
    int end = value.length();
    while (start < end) {
      int codePoint = value.codePointAt(start);
      if (!isWhiteSpace(codePoint)) {
        break;
      }
      start += Character.charCount(codePoint);
    }
    while (start < end) {
      int codePoint = value.codePointBefore(end);
      if (!isWhiteSpace(codePoint)) {
        break;
      }
      end -= Character.charCount(codePoint);
    }
    if (start == end) {
      return null;
    }
    return start == 0 && end == value.length() ? value : value.substring(start, end);
  }

  static boolean isWhiteSpace(int codePoint) {
    return switch (codePoint) {
      case 0x0009, 0x000A, 0x000B, 0x000C, 0x000D,
          0x0020, 0x0085, 0x00A0, 0x1680,
          0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true;
      default -> codePoint >= 0x2000 && codePoint <= 0x200A;
    };
  }
}
