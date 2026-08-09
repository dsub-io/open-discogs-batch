package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.boot.ApplicationArguments;

/** Validates the optional dump month in strict {@code yyyy-MM} form. */
public class YearMonthValidator implements ArgumentValidator {

  private static final YearMonth EARLIEST_DUMP = YearMonth.of(2008, 3);

  @Override
  public ValidationResult validate(ApplicationArguments args) {
    String name = ArgType.DUMP_MONTH.getGlobalName();
    if (!args.containsOption(name)) {
      return new DefaultValidationResult();
    }

    List<String> values = args.getOptionValues(name);
    if (values == null || values.size() != 1) {
      return new DefaultValidationResult(name + " must have exactly one value");
    }

    String value = values.getFirst();
    if (!value.matches("\\d{4}-\\d{2}")) {
      return new DefaultValidationResult(name + " must use yyyy-MM format");
    }

    try {
      YearMonth target = YearMonth.parse(value);
      YearMonth current = YearMonth.now();
      if (target.isBefore(EARLIEST_DUMP) || target.isAfter(current)) {
        return new DefaultValidationResult(
            name + " must be between " + EARLIEST_DUMP + " and " + current);
      }
    } catch (DateTimeParseException exception) {
      return new DefaultValidationResult(name + " must use yyyy-MM format");
    }
    return new DefaultValidationResult();
  }
}
