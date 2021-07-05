package io.dsub.discogs.batch.argument.formatter;

import io.dsub.discogs.batch.argument.ArgType;
import java.util.Arrays;

/**
 * ArgumentFormatter that formats argument's name
 */
public class ArgumentNameFormatter implements ArgumentFormatter {

  /**
   * Formats argument name so that it does not contains any plurals. Also, it will check the
   * argument name and assign proper name from {@link ArgType}.
   *
   * @param args argument to be evaluated.
   * @return argument with formatted name, or as-is if name is unrecognizable.
   */
  @Override
  public String[] format(String[] args) {
    return Arrays.stream(args)
        .map(this::doFormat)
        .toArray(String[]::new);
  }

  private String doFormat(String arg) {
    // make lower case
    String head = arg.split("=")[0].toLowerCase();
    // remove all plurals('[sS]$')
    if (head.matches(".*[sS]$") && !ArgType.contains(head)) {
      head = head.substring(0, head.length() - 1);
    }
    // fetch type of the argument name.
    ArgType type = ArgType.getTypeOf(head);

    String value;

    // meaning the last character is equals sign
    if (arg.indexOf('=') == arg.length() - 1 || arg.indexOf('=') == -1) {
      value = "";
    } else {
      // parse the value string from given argument.
      value = arg.substring(arg.indexOf("=") + 1);
    }

    // unknown type, hence return argument as-is.
    if (type == null) {
      return arg;
    }

    head = type.getGlobalName();

    // handle no-arg option
    if (!type.isValueRequired()) {
      return head;
    }

    return String.join("=", head, value);
  }
}
