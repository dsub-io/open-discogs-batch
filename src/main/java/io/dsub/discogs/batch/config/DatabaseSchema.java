package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;

/** A validated PostgreSQL schema selected by the operator. */
public record DatabaseSchema(String name) {

  public static final String DEFAULT_NAME = "public";
  public static final String INVALID_NAME_MESSAGE =
      "database-schema must be 1 to 63 lowercase letters, digits, or underscores and start with a letter or underscore";
  private static final int MAXIMUM_NAME_LENGTH = 63;
  private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]*$");

  public DatabaseSchema {
    if (!isValidName(name)) {
      throw new InvalidArgumentException(INVALID_NAME_MESSAGE);
    }
  }

  public static DatabaseSchema from(ApplicationArguments arguments) {
    String option = ArgType.DATABASE_SCHEMA.getGlobalName();
    if (!arguments.containsOption(option)) {
      return new DatabaseSchema(DEFAULT_NAME);
    }
    List<String> values = arguments.getOptionValues(option);
    if (values == null || values.size() != 1) {
      throw new InvalidArgumentException("database-schema must have exactly one value");
    }
    return new DatabaseSchema(values.getFirst());
  }

  public static boolean isValidName(String name) {
    return name != null
        && !name.isBlank()
        && name.length() <= MAXIMUM_NAME_LENGTH
        && NAME_PATTERN.matcher(name).matches();
  }

  public boolean isPublic() {
    return DEFAULT_NAME.equals(name);
  }

  public String quotedName() {
    return '"' + name + '"';
  }

  public String connectionInitializationSql() {
    return "SET search_path TO " + quotedName() + ", \"" + DEFAULT_NAME + "\"";
  }
}
