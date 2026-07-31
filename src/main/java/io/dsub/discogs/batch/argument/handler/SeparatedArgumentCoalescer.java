package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.List;

final class SeparatedArgumentCoalescer {

  String[] coalesce(String[] sourceArgs) {
    List<String> result = new ArrayList<>();
    for (int index = 0; index < sourceArgs.length; index++) {
      String argument = sourceArgs[index];
      String name = argument.split("=", 2)[0].replaceFirst("^-+", "");
      ArgType type = ArgType.getTypeOf(name);
      if (type == null || !type.isValueRequired() || argument.contains("=")) {
        result.add(argument);
        continue;
      }
      if (index + 1 >= sourceArgs.length || sourceArgs[index + 1].startsWith("-")) {
        throw new InvalidArgumentException(name + " requires a value");
      }
      result.add(argument + "=" + sourceArgs[++index]);
    }
    return result.toArray(String[]::new);
  }
}
