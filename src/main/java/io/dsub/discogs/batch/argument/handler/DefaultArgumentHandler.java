package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.argument.formatter.ArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.ArgumentNameFormatter;
import io.dsub.discogs.batch.argument.formatter.CompositeArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.FlagRemovingArgumentFormatter;
import io.dsub.discogs.batch.argument.formatter.JdbcUrlFormatter;
import io.dsub.discogs.batch.argument.validator.ArgumentValidator;
import io.dsub.discogs.batch.argument.validator.CompositeArgumentValidator;
import io.dsub.discogs.batch.argument.validator.DataSourceArgumentValidator;
import io.dsub.discogs.batch.argument.validator.DefaultDatabaseConnectionValidator;
import io.dsub.discogs.batch.argument.validator.KnownArgumentValidator;
import io.dsub.discogs.batch.argument.validator.MappedValueValidator;
import io.dsub.discogs.batch.argument.validator.TypeArgumentValidator;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.argument.validator.YearMonthValidator;
import io.dsub.discogs.batch.datasource.DBType;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * A default implementation of {@link ArgumentHandler}.
 */
@Slf4j
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

  /**
   * Default no-arg constructor.
   */
  public DefaultArgumentHandler() {
    CompositeArgumentValidator validator =
        new CompositeArgumentValidator()
            .addValidator(new DataSourceArgumentValidator())
            .addValidator(new DefaultDatabaseConnectionValidator())
            .addValidator(new KnownArgumentValidator())
            .addValidator(new MappedValueValidator())
            .addValidator(new TypeArgumentValidator())
            .addValidator(new YearMonthValidator());
    CompositeArgumentFormatter formatter =
        new CompositeArgumentFormatter()
            .addFormatter(new FlagRemovingArgumentFormatter())
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
      log.error(String.join(",", String.join(",", validationResult.getIssues())));
      return null;
    }

    String[] finalized = getSourceArgsWithDriverClassName(arguments);
    finalized = addFlags(finalized);

    return finalized;
  }

  public String[] addFlags(String[] args) {
    String[] normalized = new String[args.length];
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (!arg.matches("(^--).*")) {
        normalized[i] = "--" + arg;
        continue;
      }
      normalized[i] = arg;
    }
    return normalized;
  }

  public String[] getSourceArgsWithDriverClassName(ApplicationArguments arguments) {

    String driverClassNameHeader = "driver-class-name=";

    List<String> args = new ArrayList<>(List.of(arguments.getSourceArgs()));

    if (args.stream().anyMatch(arg -> arg.startsWith(driverClassNameHeader))) {
      return args.toArray(String[]::new);
    }

    String driverClassName = null;

    loop:
    for (String arg : args) {
      if (!arg.startsWith("--url=")) {
        continue;
      }
      for (DBType type : DBType.values()) {
        if (arg.matches(".*" + type.value() + ".*")) {
          driverClassName = type.getDriverClassName();
          break loop;
        }
      }
    }

    args.add(driverClassNameHeader + driverClassName);
    return args.toArray(String[]::new);
  }

  /**
   * Generates new instance of {@link ApplicationArguments} with normalized key and value.
   *
   * @param args to be normalized.
   * @return normalized instance.
   */
  public ApplicationArguments normalizeArguments(ApplicationArguments args) {

    String[] formattedArgs = argumentFormatter.format(args.getSourceArgs());

    List<String> normalizedResult =
        Arrays.stream(formattedArgs)
            .map(splitMultiValues)
            .reduce(
                new ArrayList<>(),
                (l, r) -> {
                  l.addAll(r);
                  return l;
                })
            .stream()
            .map(arg -> "--" + arg)
            .collect(Collectors.toList());
    return new DefaultApplicationArguments(normalizedResult.toArray(String[]::new));
  }
}