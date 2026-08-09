package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.Locale;
import java.util.Set;

final class LegacyDatabaseArgumentRejector {

  private static final Set<String> LEGACY_NAMES =
      Set.of("url", "username", "user", "u", "password", "pass", "p");

  void validate(String[] sourceArgs) {
    for (String argument : sourceArgs) {
      String name =
          argument
              .split("=", 2)[0]
              .replaceFirst("^-+", "")
              .toLowerCase(Locale.ROOT);
      if (LEGACY_NAMES.contains(name)) {
        throw new InvalidArgumentException(
            "legacy database options are not supported; use --database-url");
      }
    }
  }
}
