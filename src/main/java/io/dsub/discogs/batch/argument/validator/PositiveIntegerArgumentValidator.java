package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.argument.PositiveIntegerParser;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;

/** Validates public numeric options that must fit in a positive Java integer. */
public class PositiveIntegerArgumentValidator implements ArgumentValidator {

  private static final Set<ArgType> POSITIVE_INTEGER_ARGUMENTS =
      EnumSet.of(ArgType.CHUNK_SIZE, ArgType.MAX_WORKERS);

  @Override
  public ValidationResult validate(ApplicationArguments args) {
    ValidationResult result = new DefaultValidationResult();
    for (ArgType type : POSITIVE_INTEGER_ARGUMENTS) {
      String name = type.getGlobalName();
      if (!args.containsOption(name)) {
        continue;
      }
      List<String> values = args.getOptionValues(name);
      if (values == null) {
        continue;
      }
      for (String value : values) {
        if (!PositiveIntegerParser.isIntegerLiteral(value)) {
          continue;
        }
        OptionalInt parsed = PositiveIntegerParser.parse(value);
        if (parsed.isEmpty() || parsed.getAsInt() <= 0) {
          result = result.withIssues(publicName(name) + " must be a positive integer");
        }
      }
    }
    return result;
  }

  private String publicName(String value) {
    return value
        .replaceAll("([a-z])([A-Z])", "$1-$2")
        .toLowerCase(Locale.ROOT);
  }
}
