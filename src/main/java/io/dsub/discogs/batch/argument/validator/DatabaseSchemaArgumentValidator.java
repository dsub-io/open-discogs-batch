package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.config.DatabaseSchema;
import java.util.List;
import org.springframework.boot.ApplicationArguments;

/** Validates the optional PostgreSQL schema name before application startup. */
public final class DatabaseSchemaArgumentValidator implements ArgumentValidator {

  @Override
  public ValidationResult validate(ApplicationArguments arguments) {
    String option = ArgType.DATABASE_SCHEMA.getGlobalName();
    if (!arguments.containsOption(option)) {
      return new DefaultValidationResult();
    }
    List<String> values = arguments.getOptionValues(option);
    if (values == null || values.size() != 1) {
      return new DefaultValidationResult("database-schema must have exactly one value");
    }
    if (!DatabaseSchema.isValidName(values.getFirst())) {
      return new DefaultValidationResult(DatabaseSchema.INVALID_NAME_MESSAGE);
    }
    return new DefaultValidationResult();
  }
}
