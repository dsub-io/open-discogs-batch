package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.argument.formatter.ArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.ArgumentNameFormatter;
import io.dsub.discogs.batch.argument.formatter.CompositeArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.JdbcUrlFormatter;
import io.dsub.discogs.batch.argument.validator.ArgumentValidator;
import io.dsub.discogs.batch.argument.validator.CompositeArgumentValidator;
import io.dsub.discogs.batch.argument.validator.DataSourceArgumentValidator;
import io.dsub.discogs.batch.argument.validator.KnownArgumentValidator;
import io.dsub.discogs.batch.argument.validator.MappedValueValidator;
import io.dsub.discogs.batch.argument.validator.TypeArgumentValidator;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.argument.validator.YearMonthValidator;
import io.dsub.discogs.common.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** A default implementation of {@link ArgumentHandler}. */
@Primary
@Component
public class DefaultArgumentHandler implements ArgumentHandler {

  private final ArgumentFormatter argumentFormatter;
  private final ArgumentValidator argumentValidator;
  /**
   * Splits multiple values for OPTIONS into individual entries. It will pass through the
   * NonOptional arguments even if it has several arguments with ',' delimiters.
   */
  private final Function<String, List<String>> splitMultiValues =
      arg -> {
        if (arg.matches("^--.*") && arg.indexOf('=') < arg.length() && arg.indexOf('=') > 0) {
          String flagHead = arg.substring(0, arg.indexOf("="));
          String valueString = arg.substring(arg.indexOf("=") + 1);
          return List.of(valueString.split(",")).stream()
              .map(value -> String.join("=", flagHead, value))
              .collect(Collectors.toList());
        }
        return List.of(arg);
      };

  /** Default no-arg constructor. */
  public DefaultArgumentHandler() {
    CompositeArgumentValidator validator =
        new CompositeArgumentValidator()
            .addValidator(new DataSourceArgumentValidator())
            .addValidator(new KnownArgumentValidator())
            .addValidator(new MappedValueValidator())
            .addValidator(new TypeArgumentValidator())
            .addValidator(new YearMonthValidator());
    CompositeArgumentFormatter formatter =
        new CompositeArgumentFormatter()
            .addFormatter(new ArgumentNameFormatter())
            .addFormatter(new JdbcUrlFormatter());
    this.argumentValidator = validator;
    this.argumentFormatter = formatter;
  }

  /**
   * Resolves url formatting if the entry exists. Then passes everything into a validator to perform
   * actual validation.
   *
   * @param args given arguments.
   * @return resolved arguments.
   * @throws InvalidArgumentException if any issue exists on validation result.
   */
  @Override
  public String[] resolve(String[] args) throws InvalidArgumentException {

    ApplicationArguments arguments = normalizeArguments(new DefaultApplicationArguments(args));
    ValidationResult validationResult = this.argumentValidator.validate(arguments);

    if (!validationResult.isValid()) {
      String reasons = String.join(",", "{" + validationResult.getIssues() + "}");
      throw new InvalidArgumentException(reasons);
    }

    return arguments.getSourceArgs();
  }

  /**
   * Generates new instance of {@link ApplicationArguments} with normalized key and value.
   *
   * @param args to be normalized.
   * @return normalized instance.
   */
  public ApplicationArguments normalizeArguments(ApplicationArguments args) {
    List<String> normalizedResult =
        Arrays.stream(args.getSourceArgs())
            .map(argumentFormatter::format)
            .map(splitMultiValues)
            .reduce(
                new ArrayList<>(),
                (l, r) -> {
                  l.addAll(r);
                  return l;
                });
    return new DefaultApplicationArguments(normalizedResult.toArray(new String[0]));
  }
}
