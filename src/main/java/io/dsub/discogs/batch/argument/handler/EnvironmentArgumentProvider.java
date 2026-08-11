package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EnvironmentArgumentProvider {

  private static final String PREFIX = "OPEN_DISCOGS_BATCH_";
  private static final List<Binding> BINDINGS =
      List.of(
          new Binding("DATABASE_URL", "database-url", ArgType.URL, false),
          new Binding("DATABASE_SCHEMA", "database-schema", ArgType.DATABASE_SCHEMA, false),
          new Binding("ENTITIES", "entities", ArgType.TYPE, false),
          new Binding("DUMP_MONTH", "dump-month", ArgType.DUMP_MONTH, false),
          new Binding("DATA_DIR", "data-dir", ArgType.DATA_DIR, false),
          new Binding("CHUNK_SIZE", "chunk-size", ArgType.CHUNK_SIZE, false),
          new Binding("MAX_WORKERS", "max-workers", ArgType.MAX_WORKERS, false),
          new Binding("CLEANUP", "cleanup", ArgType.CLEANUP, true),
          new Binding("FORCE", "force", ArgType.FORCE, true),
          new Binding("ALLOW_DOWNGRADE", "allow-downgrade", ArgType.ALLOW_DOWNGRADE, true));

  private final Map<String, String> environment;

  EnvironmentArgumentProvider(Map<String, String> environment) {
    this.environment = Map.copyOf(environment);
  }

  String[] apply(String[] sourceArgs) {
    List<String> result = new ArrayList<>(List.of(sourceArgs));
    for (Binding binding : BINDINGS) {
      if (contains(sourceArgs, binding.type())) {
        continue;
      }
      String value = environment.get(PREFIX + binding.environmentName());
      if (value == null || value.isBlank()) {
        continue;
      }
      if (binding.flag()) {
        if (parseBoolean(binding.environmentName(), value)) {
          result.add("--" + binding.cliName());
        }
      } else {
        result.add("--" + binding.cliName() + "=" + value);
      }
    }
    return result.toArray(String[]::new);
  }

  private boolean contains(String[] sourceArgs, ArgType type) {
    for (String argument : sourceArgs) {
      String name = argument.split("=", 2)[0].replaceFirst("^-+", "");
      if (ArgType.getTypeOf(name) == type) {
        return true;
      }
    }
    return false;
  }

  private boolean parseBoolean(String name, String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "true", "1", "yes", "on" -> true;
      case "false", "0", "no", "off" -> false;
      default ->
          throw new InvalidArgumentException(
              PREFIX + name + " must be a boolean value, but got: " + value);
    };
  }

  private record Binding(String environmentName, String cliName, ArgType type, boolean flag) {}
}
