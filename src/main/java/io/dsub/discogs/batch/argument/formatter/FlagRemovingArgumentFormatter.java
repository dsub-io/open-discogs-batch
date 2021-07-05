package io.dsub.discogs.batch.argument.formatter;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes all - or -- flags from arguments
 */
public class FlagRemovingArgumentFormatter implements ArgumentFormatter {

  private static final Pattern PATTERN = Pattern.compile("[-]*(.*)");

  @Override
  public String[] format(String[] args) {
    if (args == null) {
      return null;
    }

    return Arrays.stream(args)
        .map(this::doFormat)
        .toArray(String[]::new);
  }

  private String doFormat(String arg) {
    if (arg == null || arg.isBlank()) {
      return arg;
    }
    Matcher m = PATTERN.matcher(arg);
    if (m.matches()) {
      return m.group(1);
    }
    return arg;
  }
}
