package io.dsub.discogs.batch.argument;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Parses public integer options that must be greater than zero. */
public final class PositiveIntegerParser {

  private static final Pattern INTEGER_LITERAL = Pattern.compile("^-?\\d+$");

  private PositiveIntegerParser() {}

  public static boolean isIntegerLiteral(String value) {
    return value != null && INTEGER_LITERAL.matcher(value).matches();
  }

  public static OptionalInt parse(String value) {
    if (!isIntegerLiteral(value)) {
      return OptionalInt.empty();
    }
    try {
      return OptionalInt.of(Integer.parseInt(value));
    } catch (NumberFormatException exception) {
      return OptionalInt.empty();
    }
  }

  public static int require(String optionName, String value) {
    OptionalInt parsed = parse(value);
    if (parsed.isEmpty() || parsed.getAsInt() <= 0) {
      throw new InvalidArgumentException(optionName + " must be a positive integer");
    }
    return parsed.getAsInt();
  }
}
